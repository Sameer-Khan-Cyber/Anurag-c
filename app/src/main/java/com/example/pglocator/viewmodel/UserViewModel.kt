package com.example.pglocator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pglocator.data.model.Enquiry
import com.example.pglocator.data.model.PGListing
import com.example.pglocator.data.repository.PGRepository
import com.example.pglocator.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserUiState(
    val listings: List<PGListing> = emptyList(),
    val enquiries: List<Enquiry> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 25000.0,
    val selectedAmenities: Set<String> = emptySet(),
    val hideOccupied: Boolean = true, // Hide occupied PGs by default
    val selectedListing: PGListing? = null,
    val errorMessage: String? = null,
    val enquirySuccess: String? = null
)

class UserViewModel(
    private val repository: PGRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadApprovedListings()
        loadMyEnquiries()
    }

    fun loadApprovedListings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val amenitiesCsv = if (_uiState.value.selectedAmenities.isNotEmpty()) {
                _uiState.value.selectedAmenities.joinToString(",")
            } else null

            val result = repository.searchPGs(
                location = _uiState.value.searchQuery.ifBlank { null },
                minPrice = if (_uiState.value.minPrice > 0) _uiState.value.minPrice else null,
                maxPrice = if (_uiState.value.maxPrice < 25000.0) _uiState.value.maxPrice else null,
                amenities = amenitiesCsv,
                includeOccupied = !_uiState.value.hideOccupied
            )

            result.fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(listings = list, isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load PG listings"
                    )
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadApprovedListings()
    }

    fun updatePriceRange(min: Double, max: Double) {
        _uiState.value = _uiState.value.copy(minPrice = min, maxPrice = max)
        loadApprovedListings()
    }

    fun toggleAmenity(amenity: String) {
        val current = _uiState.value.selectedAmenities.toMutableSet()
        if (current.contains(amenity)) {
            current.remove(amenity)
        } else {
            current.add(amenity)
        }
        _uiState.value = _uiState.value.copy(selectedAmenities = current)
        loadApprovedListings()
    }

    fun toggleHideOccupied() {
        _uiState.value = _uiState.value.copy(hideOccupied = !_uiState.value.hideOccupied)
        loadApprovedListings()
    }

    fun selectListing(listing: PGListing?) {
        _uiState.value = _uiState.value.copy(selectedListing = listing)
    }

    fun sendEnquiry(pgId: Int, message: String) {
        val user = sessionManager.getCurrentUser()
        val userId = user?.id ?: 3
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, enquirySuccess = null)
            val result = repository.sendEnquiry(pgId, userId, message.trim())
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        enquirySuccess = msg
                    )
                    loadMyEnquiries()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to send enquiry"
                    )
                }
            )
        }
    }

    fun loadMyEnquiries() {
        val user = sessionManager.getCurrentUser()
        val userId = user?.id ?: 3
        viewModelScope.launch {
            val result = repository.getUserEnquiries(userId)
            result.onSuccess { list ->
                _uiState.value = _uiState.value.copy(enquiries = list)
            }
        }
    }

    fun clearEnquirySuccess() {
        _uiState.value = _uiState.value.copy(enquirySuccess = null)
    }
}
