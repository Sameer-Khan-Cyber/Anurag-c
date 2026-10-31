package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pglocator.data.repository.PGRepository
import com.example.pglocator.ui.dialogs.SettingsDialog
import com.example.pglocator.ui.screens.*
import com.example.pglocator.util.SessionManager
import com.example.pglocator.viewmodel.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager(applicationContext)
        val repository = PGRepository(sessionManager)

        val authViewModel = AuthViewModel(repository, sessionManager)
        val userViewModel = UserViewModel(repository, sessionManager)
        val ownerViewModel = OwnerViewModel(repository, sessionManager)

        setContent {
            MyApplicationTheme {
                PGLocatorApp(
                    sessionManager = sessionManager,
                    authViewModel = authViewModel,
                    userViewModel = userViewModel,
                    ownerViewModel = ownerViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PGLocatorApp(
    sessionManager: SessionManager,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    ownerViewModel: OwnerViewModel
) {
    val authState by authViewModel.uiState.collectAsState()
    var userBottomTab by remember { mutableIntStateOf(0) } // 0: Search, 1: Enquiries
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currentRole = authState.currentUser?.role ?: sessionManager.getRole()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "FIND MY PG Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "FIND MY PG",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (authState.isLoggedIn) {
                                Text(
                                    text = if (currentRole.equals("owner", ignoreCase = true)) {
                                        "Owner Portal"
                                    } else {
                                        "Tenant Discovery"
                                    },
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (authState.isLoggedIn) {
                        // Current Role Badge
                        Surface(
                            color = if (currentRole.equals("owner", ignoreCase = true)) {
                                BrandAccent
                            } else {
                                BrandPrimaryLight
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = if (currentRole.equals("owner", ignoreCase = true)) "OWNER" else "TENANT",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Show bottom navigation only for logged in Tenant / User role
            if (authState.isLoggedIn && currentRole.equals("user", ignoreCase = true)) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = userBottomTab == 0,
                        onClick = { userBottomTab = 0 },
                        icon = { Icon(Icons.Default.Search, contentDescription = "Search PGs") },
                        label = { Text("Discover") },
                        modifier = Modifier.testTag("nav_search")
                    )
                    NavigationBarItem(
                        selected = userBottomTab == 1,
                        onClick = {
                            userBottomTab = 1
                            userViewModel.loadMyEnquiries()
                        },
                        icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = "My Enquiries") },
                        label = { Text("My Enquiries") },
                        modifier = Modifier.testTag("nav_enquiries")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!authState.isLoggedIn) {
                // Auth View
                AuthScreen(
                    viewModel = authViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // RoleRouter: routes strictly between owner and tenant/user
                if (currentRole.equals("owner", ignoreCase = true)) {
                    OwnerHomeScreen(
                        viewModel = ownerViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // User / Tenant Dashboard
                    when (userBottomTab) {
                        0 -> UserSearchScreen(
                            viewModel = userViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        1 -> UserEnquiriesScreen(
                            viewModel = userViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Settings & Role Switcher Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            sessionManager = sessionManager,
            currentRole = currentRole,
            onRoleChange = { newRole ->
                authViewModel.switchRole(newRole)
                showSettingsDialog = false
            },
            onLogout = {
                authViewModel.logout()
                showSettingsDialog = false
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
