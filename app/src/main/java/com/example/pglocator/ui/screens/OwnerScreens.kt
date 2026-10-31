package com.example.pglocator.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pglocator.data.model.Enquiry
import com.example.pglocator.data.model.PGListing
import com.example.pglocator.ui.components.InteractiveMapView
import com.example.pglocator.viewmodel.OwnerViewModel
import com.example.ui.theme.*

@Composable
fun OwnerHomeScreen(
    viewModel: OwnerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Navigation Bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My Listings (${uiState.myListings.size})") },
                icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("owner_tab_listings")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Post New PG") },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("owner_tab_post")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    BadgedBox(
                        badge = {
                            val pendingCount = uiState.incomingEnquiries.count { it.status == "pending" }
                            if (pendingCount > 0) {
                                Badge { Text("$pendingCount") }
                            }
                        }
                    ) {
                        Text("Inquiries")
                    }
                },
                icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("owner_tab_inquiries")
            )
        }

        uiState.successMessage?.let { msg ->
            Surface(
                color = StatusApprovedBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusApproved, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = msg, fontSize = 12.sp, color = StatusApproved, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.resetPostSuccess() },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = StatusApproved, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> OwnerListingsTab(
                    listings = uiState.myListings,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.loadMyListings() },
                    onAddNewClick = { selectedTab = 1 },
                    onToggleOccupancy = { id, occupied -> viewModel.toggleOccupancy(id, occupied) }
                )
                1 -> PostNewPGTab(
                    viewModel = viewModel,
                    onPostSuccess = { selectedTab = 0 }
                )
                2 -> OwnerInquiriesTab(
                    enquiries = uiState.incomingEnquiries,
                    onRefresh = { viewModel.loadIncomingEnquiries() },
                    onReply = { id, reply -> viewModel.replyToEnquiry(id, reply) }
                )
            }
        }
    }
}

@Composable
fun OwnerListingsTab(
    listings: List<PGListing>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onAddNewClick: () -> Unit,
    onToggleOccupancy: (pgId: Int, markOccupied: Boolean) -> Unit
) {
    val vacantCount = listings.count { it.isVacant }
    val occupiedCount = listings.count { !it.isVacant }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My PG Accommodations",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (listings.isNotEmpty()) {
                    Text(
                        text = "$vacantCount Available (Visible) • $occupiedCount Occupied (Hidden)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (listings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Apartment,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No PG listings posted yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddNewClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Post Your First PG")
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(listings, key = { it.id }) { listing ->
                    OwnerListingItemCard(
                        listing = listing,
                        onToggleOccupancy = onToggleOccupancy
                    )
                }
            }
        }
    }
}

@Composable
fun OwnerListingItemCard(
    listing: PGListing,
    onToggleOccupancy: (pgId: Int, markOccupied: Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = listing.imageUrl ?: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=400&q=80",
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = listing.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        val (badgeText, badgeBg, badgeTextColor) = when (listing.status.lowercase()) {
                            "approved" -> Triple("Approved", StatusApprovedBg, StatusApproved)
                            "rejected" -> Triple("Rejected", StatusRejectedBg, StatusRejected)
                            else -> Triple("Pending Review", StatusPendingBg, StatusPending)
                        }

                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "₹${listing.monthlyRent.toInt()}/month",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BrandPrimary
                        )
                        Text(
                            text = "ID: #${listing.id}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Occupancy Control Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (listing.isVacant) {
                        Surface(
                            color = StatusApprovedBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusApproved,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Available / Vacant",
                                    color = StatusApproved,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Visible to other users",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Surface(
                            color = StatusRejectedBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = StatusRejected,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Occupied / Full",
                                    color = StatusRejected,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Removed from other users until empty",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (listing.isVacant) {
                    OutlinedButton(
                        onClick = { onToggleOccupancy(listing.id, true) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_occupy_${listing.id}")
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hide (Mark Occupied)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Button(
                        onClick = { onToggleOccupancy(listing.id, false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusApproved
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_vacate_${listing.id}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Empty (Show)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PostNewPGTab(
    viewModel: OwnerViewModel,
    onPostSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            viewModel.uploadImage(context, uri)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.fetchCurrentLocation(context)
        } else {
            // Still attempt to obtain network/last known location gracefully
            viewModel.fetchCurrentLocation(context)
        }
    }

    val availableAmenities = listOf("WiFi", "AC", "Food", "Gym", "Laundry", "Security", "Power Backup")

    // Handle post success feedback
    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) {
            viewModel.resetPostSuccess()
            onPostSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Publish PG Accommodation",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Listings are published immediately and become discoverable by prospective tenants.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image Upload Section
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.uploadedImageUrl.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = uiState.uploadedImageUrl,
                            contentDescription = "Uploaded PG",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = StatusApprovedBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Uploaded via Multipart",
                                color = StatusApproved,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    enabled = !uiState.isUploadingImage,
                    modifier = Modifier.testTag("upload_image_btn")
                ) {
                    if (uiState.isUploadingImage) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Uploading Image (Multipart)...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.uploadedImageUrl.isBlank()) "Select & Upload PG Image" else "Change Image")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Form
        OutlinedTextField(
            value = uiState.title,
            onValueChange = { viewModel.updateFormField(title = it) },
            label = { Text("PG Title *") },
            placeholder = { Text("e.g. Royal Comfort Executive PG for Men") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("post_pg_title_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = uiState.monthlyRent,
                onValueChange = { viewModel.updateFormField(rent = it) },
                label = { Text("Monthly Rent (₹) *") },
                placeholder = { Text("12000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("post_pg_rent_input")
            )

            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.updateFormField(address = it) },
                label = { Text("Street / City *") },
                placeholder = { Text("Koramangala, Bengaluru") },
                singleLine = true,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("post_pg_address_input")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { viewModel.updateFormField(description = it) },
            label = { Text("Description") },
            placeholder = { Text("Describe room sharing types, hygiene, nearby landmarks...") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = uiState.rules,
            onValueChange = { viewModel.updateFormField(rules = it) },
            label = { Text("House Rules") },
            placeholder = { Text("e.g. Gate closes at 11 PM, Visitors policy...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Amenities Checkboxes
        Text(
            text = "Select Available Amenities",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        availableAmenities.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { amenity ->
                    val checked = uiState.selectedAmenities.contains(amenity)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.toggleAmenity(amenity) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { viewModel.toggleAmenity(amenity) }
                        )
                        Text(text = amenity, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Section with Current Location Button & Map
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PG Location & Coordinates",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Use your device GPS or pinpoint on the map below.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent "Use My Current Location" Button
        FilledTonalButton(
            onClick = {
                val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED) {
                    viewModel.fetchCurrentLocation(context)
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("use_current_location_btn"),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = BrandPrimaryContainer,
                contentColor = BrandOnPrimaryContainer
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLocating
        ) {
            if (uiState.isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detecting Current GPS Location...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current Location",
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use My Current Location", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected GPS Coordinates Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PinDrop,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Selected Coordinates:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format("%.6f", uiState.latitude)}, ${String.format("%.6f", uiState.longitude)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    color = StatusApprovedBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "GPS Ready",
                        color = StatusApproved,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        InteractiveMapView(
            latitude = uiState.latitude,
            longitude = uiState.longitude,
            isInteractive = true,
            title = "Pinpoint PG Location",
            onLocationChanged = { lat, lng ->
                viewModel.setLocation(lat, lng)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Error message if any
        uiState.errorMessage?.let { error ->
            Surface(
                color = StatusRejectedBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error,
                    color = StatusRejected,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Submit Button
        Button(
            onClick = { viewModel.submitNewPG() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("submit_post_pg_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.Default.Publish, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish PG Listing", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun OwnerInquiriesTab(
    enquiries: List<Enquiry>,
    onRefresh: () -> Unit,
    onReply: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tenant Inquiries",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        if (enquiries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MarkEmailRead,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No incoming inquiries", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Prospective tenant queries will appear here.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(enquiries, key = { it.id }) { enquiry ->
                    OwnerEnquiryItemCard(enquiry = enquiry, onReply = { reply -> onReply(enquiry.id, reply) })
                }
            }
        }
    }
}

@Composable
fun OwnerEnquiryItemCard(
    enquiry: Enquiry,
    onReply: (String) -> Unit
) {
    var isReplying by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("owner_enquiry_card_${enquiry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = enquiry.pgTitle ?: "PG Property",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandPrimary,
                    modifier = Modifier.weight(1f)
                )

                val isReplied = enquiry.status == "replied"
                Surface(
                    color = if (isReplied) StatusApprovedBg else StatusPendingBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isReplied) "Replied" else "Action Needed",
                        color = if (isReplied) StatusApproved else StatusPending,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // User info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${enquiry.userName ?: "Prospective Tenant"} (${enquiry.userPhone ?: ""})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inquiry message
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${enquiry.message}\"",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Existing owner reply if any
            if (!enquiry.ownerReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = BrandPrimaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "Your Reply:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                            Text(text = enquiry.ownerReply, fontSize = 12.sp, color = BrandOnPrimaryContainer)
                        }
                    }
                }
            }

            // Reply Form / Toggle
            if (!isReplying) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { isReplying = true },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (enquiry.ownerReply.isNullOrBlank()) "Reply to Tenant" else "Update Reply", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Type your response to the prospective tenant...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isReplying = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                onReply(replyText)
                                isReplying = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        enabled = replyText.isNotBlank()
                    ) {
                        Text("Send Reply")
                    }
                }
            }
        }
    }
}
