package com.example.pglocator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.pglocator.data.model.Enquiry
import com.example.pglocator.data.model.PGListing
import com.example.pglocator.ui.components.InteractiveMapView
import com.example.pglocator.viewmodel.UserViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    viewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedPGForDetail by remember { mutableStateOf<PGListing?>(null) }
    var pgForEnquiry by remember { mutableStateOf<PGListing?>(null) }

    val allAmenities = listOf("WiFi", "AC", "Food", "Gym", "Laundry", "Security", "Power Backup")

    Column(modifier = modifier.fillMaxSize()) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search by area, PG name, city...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandPrimary)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.selectedAmenities.isNotEmpty() || uiState.maxPrice < 25000.0)
                                    BrandAccentContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("filter_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (uiState.selectedAmenities.isNotEmpty()) {
                                    Badge { Text("${uiState.selectedAmenities.size}") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filters",
                                tint = if (uiState.selectedAmenities.isNotEmpty()) BrandAccent else BrandPrimary
                            )
                        }
                    }
                }

                // Quick Filter Chips (Occupancy + Amenities)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.hideOccupied,
                            onClick = { viewModel.toggleHideOccupied() },
                            label = {
                                Text(
                                    text = if (uiState.hideOccupied) "Occupied Hidden" else "All (Incl. Occupied)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (uiState.hideOccupied) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimaryContainer,
                                selectedLabelColor = BrandOnPrimaryContainer
                            ),
                            modifier = Modifier.testTag("filter_hide_occupied")
                        )
                    }

                    items(allAmenities) { amenity ->
                        val isSelected = uiState.selectedAmenities.contains(amenity)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleAmenity(amenity) },
                            label = { Text(amenity, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandPrimaryContainer,
                                selectedLabelColor = BrandOnPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Listings Content
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.listings.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HomeWork,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No PG listings found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search location, price budget, or amenity filters.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.listings, key = { it.id }) { listing ->
                        PGListingCard(
                            listing = listing,
                            onClick = { selectedPGForDetail = listing },
                            onEnquiryClick = { pgForEnquiry = listing }
                        )
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet Dialog
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Filter PG Listings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Price Slider
                Text(
                    text = "Max Monthly Rent: ₹${uiState.maxPrice.toInt()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandPrimary
                )
                Slider(
                    value = uiState.maxPrice.toFloat(),
                    onValueChange = { viewModel.updatePriceRange(uiState.minPrice, it.toDouble()) },
                    valueRange = 5000f..30000f,
                    steps = 25,
                    colors = SliderDefaults.colors(thumbColor = BrandPrimary, activeTrackColor = BrandPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Occupancy filter switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hide Occupied Accommodations",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Only show vacant PGs currently available for rent",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.hideOccupied,
                        onCheckedChange = { viewModel.toggleHideOccupied() },
                        modifier = Modifier.testTag("switch_hide_occupied")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Required Amenities",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                allAmenities.forEach { amenity ->
                    val checked = uiState.selectedAmenities.contains(amenity)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAmenity(amenity) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { viewModel.toggleAmenity(amenity) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = amenity, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Apply Filters (${uiState.listings.size} Results)")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // PG Detail Dialog
    selectedPGForDetail?.let { listing ->
        PGDetailDialog(
            listing = listing,
            onDismiss = { selectedPGForDetail = null },
            onSendEnquiry = {
                selectedPGForDetail = null
                pgForEnquiry = listing
            }
        )
    }

    // Send Enquiry Dialog
    pgForEnquiry?.let { listing ->
        SendEnquiryDialog(
            pgListing = listing,
            onDismiss = { pgForEnquiry = null },
            onSubmit = { msg ->
                viewModel.sendEnquiry(listing.id, msg)
                pgForEnquiry = null
            }
        )
    }

    // Success Snack/Notification
    uiState.enquirySuccess?.let { successMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearEnquirySuccess() },
            title = { Text("Enquiry Sent!") },
            text = { Text(successMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearEnquirySuccess() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PGListingCard(
    listing: PGListing,
    onClick: () -> Unit,
    onEnquiryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("pg_card_${listing.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Photo & Verified Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                AsyncImage(
                    model = listing.imageUrl?.ifBlank { "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80" }
                        ?: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                    contentDescription = listing.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Status / Occupancy Badge
                val isOccupied = !listing.isVacant
                Surface(
                    color = if (isOccupied) StatusRejectedBg else StatusApprovedBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .padding(10.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOccupied) Icons.Default.VisibilityOff else Icons.Default.Verified,
                            contentDescription = if (isOccupied) "Occupied" else "Verified",
                            tint = if (isOccupied) StatusRejected else StatusApproved,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOccupied) "Occupied / Full" else "Available • Verified",
                            color = if (isOccupied) StatusRejected else StatusApproved,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Price Tag
                Surface(
                    color = BrandPrimary,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "₹${listing.monthlyRent.toInt()}/mo",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Info Body
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = listing.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = listing.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amenities chips
                val amenities = listing.parsedAmenities()
                if (amenities.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        amenities.take(4).forEach { amenity ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = amenity,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (amenities.size > 4) {
                            Text(
                                text = "+${amenities.size - 4} more",
                                fontSize = 11.sp,
                                color = BrandPrimary,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View on Map")
                    }

                    if (listing.isVacant) {
                        Button(
                            onClick = onEnquiryClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("send_enquiry_btn_${listing.id}")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Enquiry", fontSize = 12.sp)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("occupied_btn_${listing.id}")
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Occupied", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PGDetailDialog(
    listing: PGListing,
    onDismiss: () -> Unit,
    onSendEnquiry: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Header with Close Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    AsyncImage(
                        model = listing.imageUrl ?: "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                        contentDescription = listing.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(modifier = Modifier.padding(18.dp)) {
                    if (!listing.isVacant) {
                        Surface(
                            color = StatusRejectedBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = StatusRejected,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "This PG is currently occupied / full. The owner will make it available once rooms get empty.",
                                    fontSize = 12.sp,
                                    color = StatusRejected,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = listing.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = listing.address,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = BrandPrimaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "₹${listing.monthlyRent.toInt()}/mo",
                                color = BrandOnPrimaryContainer,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "About this Accommodation",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listing.description.ifBlank { "Modern paying guest facility equipped with contemporary amenities." },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Amenities",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRowAmenities(amenities = listing.parsedAmenities())

                    if (!listing.rules.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "House Rules",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = listing.rules,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Location & Map",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InteractiveMapView(
                        latitude = listing.latitude,
                        longitude = listing.longitude,
                        title = listing.title,
                        isInteractive = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    // Owner Contact Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = listing.ownerName ?: "Property Owner",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = listing.ownerPhone ?: "+91 98765 43210",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    if (listing.isVacant) {
                        Button(
                            onClick = onSendEnquiry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("modal_send_enquiry_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Enquiry to Owner", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("modal_send_enquiry_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Currently Occupied (Inquiries Paused)", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRowAmenities(amenities: List<String>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(amenities) { item ->
            Surface(
                color = BrandPrimaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandOnPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SendEnquiryDialog(
    pgListing: PGListing,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    val presets = listOf(
        "Hi, is double sharing room currently available?",
        "Can I schedule a property visit this weekend?",
        "Is two-wheeler and car parking space provided?",
        "What are the check-in and security deposit rules?"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enquire about PG",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = pgListing.title,
                    fontSize = 13.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Your Message to Property Owner") },
                    placeholder = { Text("Type questions about vacancy, food, rules...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enquiry_message_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Quick Questions:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                presets.forEach { preset ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { message = preset }
                            .padding(vertical = 3.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (message.isNotBlank()) onSubmit(message) },
                enabled = message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier.testTag("submit_enquiry_btn")
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UserEnquiriesScreen(
    viewModel: UserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Enquiries",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { viewModel.loadMyEnquiries() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (uiState.enquiries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No enquiries submitted yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Browse PGs and send enquiries to owners here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(uiState.enquiries, key = { it.id }) { enquiry ->
                    EnquiryItemCard(enquiry = enquiry)
                }
            }
        }
    }
}

@Composable
fun EnquiryItemCard(enquiry: Enquiry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("enquiry_card_${enquiry.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = enquiry.pgTitle ?: "Paying Guest Residence",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val isReplied = enquiry.status == "replied"
                Surface(
                    color = if (isReplied) StatusApprovedBg else StatusPendingBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isReplied) "Replied" else "Pending Reply",
                        color = if (isReplied) StatusApproved else StatusPending,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User's Query
            Row {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = enquiry.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Owner Reply
            if (!enquiry.ownerReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = BrandPrimaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Owner's Response:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = enquiry.ownerReply,
                            fontSize = 12.sp,
                            color = BrandOnPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Owner: ${enquiry.ownerName ?: "Rohit Sharma"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = enquiry.createdAt ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
