package com.example.pglocator.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pglocator.data.model.CreatePGRequest
import com.example.pglocator.data.model.Enquiry
import com.example.pglocator.data.model.PGListing
import com.example.pglocator.data.repository.PGRepository
import com.example.pglocator.util.FileUploadHelper
import com.example.pglocator.util.LocationHelper
import com.example.pglocator.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OwnerUiState(
    val myListings: List<PGListing> = emptyList(),
    val incomingEnquiries: List<Enquiry> = emptyList(),
    val isLoading: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isLocating: Boolean = false,
    val uploadedImageUrl: String = "",
    val postSuccess: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // New PG Form state
    val title: String = "",
    val description: String = "",
    val address: String = "",
    val latitude: Double = 12.9716,
    val longitude: Double = 77.5946,
    val monthlyRent: String = "",
    val selectedAmenities: Set<String> = setOf("WiFi", "Food", "Security"),
    val rules: String = "Gate closes at 11:00 PM. No smoking inside."
)

class OwnerViewModel(
    private val repository: PGRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OwnerUiState())
    val uiState: StateFlow<OwnerUiState> = _uiState.asStateFlow()

    init {
        loadMyListings()
        loadIncomingEnquiries()
    }

    fun loadMyListings() {
        val user = sessionManager.getCurrentUser()
        val ownerId = user?.id ?: 2
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.getOwnerPGs(ownerId)
            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(myListings = list, isLoading = false)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Failed to load listings"
                    )
                }
            )
        }
    }

    fun loadIncomingEnquiries() {
        val user = sessionManager.getCurrentUser()
        val ownerId = user?.id ?: 2
        viewModelScope.launch {
            val result = repository.getOwnerEnquiries(ownerId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(incomingEnquiries = list)
            }
        }
    }

    fun uploadImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true, errorMessage = null)
            val result = FileUploadHelper.uploadImageFromUri(context, imageUri)
            result.fold(
                onSuccess = { url ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingImage = false,
                        uploadedImageUrl = url,
                        successMessage = "Image uploaded successfully!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isUploadingImage = false,
                        errorMessage = error.localizedMessage ?: "Failed to upload image. Please verify your connection to the server."
                    )
                }
            )
        }
    }

    fun setLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(latitude = lat, longitude = lng)
    }

    fun fetchCurrentLocation(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocating = true, errorMessage = null)
            val result = LocationHelper.getCurrentLocation(context)
            result.fold(
                onSuccess = { loc ->
                    val updatedAddress = if (_uiState.value.address.isBlank() && loc.addressName != null) {
                        loc.addressName
                    } else _uiState.value.address

                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        address = updatedAddress,
                        successMessage = "Location updated to current GPS coordinates (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})"
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLocating = false,
                        errorMessage = "Could not fetch current GPS location: ${err.localizedMessage ?: "Unknown error"}"
                    )
                }
            )
        }
    }

    fun updateFormField(
        title: String? = null,
        description: String? = null,
        address: String? = null,
        rent: String? = null,
        rules: String? = null,
        imageUrl: String? = null
    ) {
        _uiState.value = _uiState.value.copy(
            title = title ?: _uiState.value.title,
            description = description ?: _uiState.value.description,
            address = address ?: _uiState.value.address,
            monthlyRent = rent ?: _uiState.value.monthlyRent,
            rules = rules ?: _uiState.value.rules,
            uploadedImageUrl = imageUrl ?: _uiState.value.uploadedImageUrl
        )
    }

    fun toggleAmenity(amenity: String) {
        val current = _uiState.value.selectedAmenities.toMutableSet()
        if (current.contains(amenity)) {
            current.remove(amenity)
        } else {
            current.add(amenity)
        }
        _uiState.value = _uiState.value.copy(selectedAmenities = current)
    }

    fun submitNewPG() {
        val state = _uiState.value
        val rentVal = state.monthlyRent.toDoubleOrNull()
        if (state.title.isBlank() || state.address.isBlank() || rentVal == null || rentVal <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter title, address, and a valid monthly rent amount.")
            return
        }

        val user = sessionManager.getCurrentUser()
        val ownerId = user?.id ?: 2

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val request = CreatePGRequest(
                ownerId = ownerId,
                title = state.title.trim(),
                description = state.description.trim(),
                address = state.address.trim(),
                latitude = state.latitude,
                longitude = state.longitude,
                monthlyRent = rentVal,
                amenities = state.selectedAmenities.toList(),
                rules = state.rules.trim(),
                imageUrl = state.uploadedImageUrl
            )

            val result = repository.postPG(request)
            result.fold(
                onSuccess = { created ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        postSuccess = true,
                        successMessage = "PG Listing published successfully! It is now live for tenants.",
                        title = "",
                        description = "",
                        address = "",
                        monthlyRent = "",
                        uploadedImageUrl = ""
                    )
                    loadMyListings()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.localizedMessage ?: "Failed to post listing"
                    )
                }
            )
        }
    }

    fun replyToEnquiry(enquiryId: Int, replyText: String) {
        if (replyText.isBlank()) return
        val user = sessionManager.getCurrentUser()
        val ownerId = user?.id ?: 2
        viewModelScope.launch {
            val result = repository.replyEnquiry(enquiryId, ownerId, replyText.trim())
            result.onSuccess { msg ->
                _uiState.value = _uiState.value.copy(successMessage = msg)
                loadIncomingEnquiries()
            }
        }
    }

    fun toggleOccupancy(pgId: Int, markOccupied: Boolean) {
        val user = sessionManager.getCurrentUser()
        val ownerId = user?.id ?: 2
        val newOccupiedVal = if (markOccupied) 1 else 0

        viewModelScope.launch {
            val updatedList = _uiState.value.myListings.map {
                if (it.id == pgId) it.copy(isOccupied = newOccupiedVal) else it
            }
            _uiState.value = _uiState.value.copy(myListings = updatedList)

            val result = repository.toggleOccupancy(pgId, ownerId, newOccupiedVal)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(successMessage = msg)
                    loadMyListings()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = err.localizedMessage ?: "Failed to update occupancy status"
                    )
                    loadMyListings()
                }
            )
        }
    }

    fun resetPostSuccess() {
        _uiState.value = _uiState.value.copy(postSuccess = false, successMessage = null, errorMessage = null)
    }
}
