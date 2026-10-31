package com.example.pglocator.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.pglocator.util.SessionManager
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    sessionManager: SessionManager,
    currentRole: String,
    onRoleChange: (String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(sessionManager.getBaseUrl()) }
    val user = sessionManager.getCurrentUser()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session & Server Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Info
                if (user != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = user.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current Role: ${user.role.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Switch Active Role
                Text(
                    text = "Switch Active Workspace:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentRole == "user",
                        onClick = { onRoleChange("user") },
                        label = { Text("Tenant", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("switch_to_user")
                    )
                    FilterChip(
                        selected = currentRole == "owner",
                        onClick = { onRoleChange("owner") },
                        label = { Text("Owner", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).testTag("switch_to_owner")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Base URL Config
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        sessionManager.setBaseUrl(it)
                    },
                    label = { Text("PHP Backend Base URL") },
                    placeholder = { Text("http://10.0.2.2/pg_locator/api/") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth().testTag("base_url_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRejected),
                    modifier = Modifier.fillMaxWidth().testTag("logout_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Out")
                }
            }
        }
    }
}
