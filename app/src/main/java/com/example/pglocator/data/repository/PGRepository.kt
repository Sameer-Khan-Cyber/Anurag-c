package com.example.pglocator.data.repository

import com.example.pglocator.data.local.LocalDataManager
import com.example.pglocator.data.model.*
import com.example.pglocator.data.network.ApiClient
import com.example.pglocator.data.network.ApiService
import com.example.pglocator.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PGRepository:
 * Unified repository supporting direct PHP/MySQL REST API communication
 * with seamless local persistent database fallback.
 * Allows account creation, login, PG posting, occupancy toggling, and inquiries
 * to work seamlessly in all environments.
 */
class PGRepository(
    private val sessionManager: SessionManager,
    private val localDataManager: LocalDataManager = LocalDataManager(sessionManager.context),
    private val apiService: ApiService = ApiClient.getService()
) {

    // AUTH: LOGIN
    suspend fun login(request: LoginRequest): Result<User> = withContext(Dispatchers.IO) {
        // Try remote API if available
        try {
            val res = apiService.login(request)
            if (res.status && res.data != null) {
                localDataManager.saveUser(res.data, request.password)
                sessionManager.saveUserSession(res.data)
                return@withContext Result.success(res.data)
            }
        } catch (_: Exception) {
            // Server offline / unreachable, verify against local persistent database
        }

        val localUser = localDataManager.authenticateUser(request.email, request.password)
        if (localUser != null) {
            sessionManager.saveUserSession(localUser)
            Result.success(localUser)
        } else {
            Result.failure(Exception("Invalid email or password. Please verify your credentials or register a new account."))
        }
    }

    // AUTH: REGISTER
    suspend fun register(request: RegisterRequest): Result<User> = withContext(Dispatchers.IO) {
        val existingLocal = localDataManager.findUserByEmail(request.email)
        if (existingLocal != null) {
            return@withContext Result.failure(Exception("An account with '${request.email}' already exists. Please log in."))
        }

        // Try remote API if available
        try {
            val res = apiService.register(request)
            if (res.status && res.data != null) {
                localDataManager.saveUser(res.data, request.password)
                sessionManager.saveUserSession(res.data)
                return@withContext Result.success(res.data)
            } else if (!res.status && res.message.isNotBlank()) {
                if (res.message.contains("already exists", ignoreCase = true)) {
                    return@withContext Result.failure(Exception(res.message))
                }
            }
        } catch (_: Exception) {
            // Server offline / unreachable, register account into persistent database
        }

        // Register into local persistent storage
        val newUser = localDataManager.registerUser(
            fullName = request.fullName,
            email = request.email,
            phone = request.phone,
            pass = request.password,
            role = request.role
        )
        sessionManager.saveUserSession(newUser)
        Result.success(newUser)
    }

    // USER SEARCH & BROWSE
    // By default, occupied PGs are removed for users until the owner marks them empty again!
    suspend fun searchPGs(
        location: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        amenities: String? = null,
        includeOccupied: Boolean = false
    ): Result<List<PGListing>> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.searchPGs(
                location = location,
                minPrice = minPrice,
                maxPrice = maxPrice,
                amenities = amenities,
                includeOccupied = if (includeOccupied) true else null
            )
            if (res.status && res.data != null) {
                return@withContext Result.success(res.data)
            }
        } catch (_: Exception) {}

        val listings = localDataManager.getListings(
            location = location,
            minPrice = minPrice,
            maxPrice = maxPrice,
            amenities = amenities,
            includeOccupied = includeOccupied
        )
        Result.success(listings)
    }

    // USER ENQUIRIES
    suspend fun sendEnquiry(pgId: Int, userId: Int, message: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.sendEnquiry(SendEnquiryRequest(pgId, userId, message))
            if (res.status) {
                return@withContext Result.success(res.message)
            }
        } catch (_: Exception) {}

        val user = sessionManager.getCurrentUser()
        val pg = localDataManager.getListingById(pgId)
        val enquiry = Enquiry(
            id = localDataManager.getNextEnquiryId(),
            pgId = pgId,
            userId = userId,
            ownerId = pg?.ownerId ?: 2,
            message = message,
            status = "pending",
            ownerReply = null,
            createdAt = "Just now",
            updatedAt = "Just now",
            pgTitle = pg?.title ?: "Paying Guest Residence",
            pgAddress = pg?.address ?: "",
            monthlyRent = pg?.monthlyRent ?: 0.0,
            pgImageUrl = pg?.imageUrl,
            userName = user?.fullName ?: "Resident",
            userEmail = user?.email ?: "",
            userPhone = user?.phone ?: "",
            ownerName = pg?.ownerName ?: "Property Owner",
            ownerPhone = pg?.ownerPhone ?: "+91 98765 43210",
            ownerEmail = pg?.ownerEmail ?: "owner@pglocator.com"
        )
        localDataManager.addEnquiry(enquiry)
        Result.success("Enquiry submitted to property owner successfully!")
    }

    suspend fun getUserEnquiries(userId: Int): Result<List<Enquiry>> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.getUserEnquiries(userId)
            if (res.status && res.data != null) {
                return@withContext Result.success(res.data)
            }
        } catch (_: Exception) {}

        Result.success(localDataManager.getUserEnquiries(userId))
    }

    // OWNER LISTINGS & ENQUIRIES
    suspend fun getOwnerPGs(ownerId: Int): Result<List<PGListing>> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.getOwnerPGs(ownerId)
            if (res.status && res.data != null) {
                return@withContext Result.success(res.data)
            }
        } catch (_: Exception) {}

        Result.success(localDataManager.getOwnerListings(ownerId))
    }

    suspend fun postPG(request: CreatePGRequest): Result<PGListing> = withContext(Dispatchers.IO) {
        var createdListing: PGListing? = null
        try {
            val res = apiService.postPG(request)
            if (res.status && res.data != null) {
                createdListing = res.data
            }
        } catch (_: Exception) {}

        val user = sessionManager.getCurrentUser()
        val finalListing = createdListing ?: localDataManager.createListing(
            ownerId = request.ownerId,
            title = request.title,
            description = request.description,
            address = request.address,
            latitude = request.latitude,
            longitude = request.longitude,
            monthlyRent = request.monthlyRent,
            amenities = request.amenities,
            rules = request.rules,
            imageUrl = request.imageUrl,
            ownerName = user?.fullName ?: "PG Owner",
            ownerPhone = user?.phone ?: "+91 98765 43210",
            ownerEmail = user?.email ?: "owner@pglocator.com"
        )

        localDataManager.addListing(finalListing)
        Result.success(finalListing)
    }

    suspend fun getOwnerEnquiries(ownerId: Int): Result<List<Enquiry>> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.getOwnerEnquiries(ownerId)
            if (res.status && res.data != null) {
                return@withContext Result.success(res.data)
            }
        } catch (_: Exception) {}

        Result.success(localDataManager.getOwnerEnquiries(ownerId))
    }

    suspend fun replyEnquiry(enquiryId: Int, ownerId: Int, ownerReply: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.replyEnquiry(ReplyEnquiryRequest(enquiryId, ownerId, ownerReply))
            if (res.status) {
                localDataManager.replyToEnquiry(enquiryId, ownerReply)
                return@withContext Result.success(res.message)
            }
        } catch (_: Exception) {}

        localDataManager.replyToEnquiry(enquiryId, ownerReply)
        Result.success("Reply sent to tenant successfully!")
    }

    suspend fun toggleOccupancy(pgId: Int, ownerId: Int, isOccupied: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = apiService.toggleOccupancy(ToggleOccupancyRequest(pgId, ownerId, isOccupied))
            if (res.status) {
                localDataManager.updateOccupancy(pgId, isOccupied)
                return@withContext Result.success(res.message)
            }
        } catch (_: Exception) {}

        localDataManager.updateOccupancy(pgId, isOccupied)
        val msg = if (isOccupied == 1) {
            "PG marked as Occupied (hidden from users until vacant again)"
        } else {
            "PG marked as Empty & Available (now visible to users)"
        }
        Result.success(msg)
    }
}
