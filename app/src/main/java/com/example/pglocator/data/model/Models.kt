package com.example.pglocator.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "role") val role: String = "user", // "user", "owner", "admin"
    @Json(name = "token") val token: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class PGListing(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "owner_id") val ownerId: Int = 0,
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "address") val address: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "monthly_rent") val monthlyRent: Double = 0.0,
    @Json(name = "amenities") val amenities: String = "",
    @Json(name = "rules") val rules: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "is_occupied") val isOccupied: Int = 0,
    @Json(name = "status") val status: String = "approved", // "pending", "approved", "rejected"
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "owner_name") val ownerName: String? = null,
    @Json(name = "owner_phone") val ownerPhone: String? = null,
    @Json(name = "owner_email") val ownerEmail: String? = null,
    @Json(name = "amenities_list") val amenitiesList: List<String>? = null
) {
    val isVacant: Boolean get() = isOccupied == 0
    fun parsedAmenities(): List<String> {
        if (!amenitiesList.isNullOrEmpty()) return amenitiesList
        if (amenities.isBlank()) return emptyList()
        return try {
            if (amenities.startsWith("[")) {
                amenities.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                    .filter { it.isNotBlank() }
            } else {
                amenities.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@JsonClass(generateAdapter = true)
data class Enquiry(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "pg_id") val pgId: Int = 0,
    @Json(name = "user_id") val userId: Int = 0,
    @Json(name = "owner_id") val ownerId: Int = 0,
    @Json(name = "message") val message: String = "",
    @Json(name = "status") val status: String = "pending", // "pending", "replied", "closed"
    @Json(name = "owner_reply") val ownerReply: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "pg_title") val pgTitle: String? = null,
    @Json(name = "pg_address") val pgAddress: String? = null,
    @Json(name = "monthly_rent") val monthlyRent: Double? = null,
    @Json(name = "pg_image_url") val pgImageUrl: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "user_email") val userEmail: String? = null,
    @Json(name = "user_phone") val userPhone: String? = null,
    @Json(name = "owner_name") val ownerName: String? = null,
    @Json(name = "owner_phone") val ownerPhone: String? = null,
    @Json(name = "owner_email") val ownerEmail: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    @Json(name = "status") val status: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "password") val password: String,
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class CreatePGRequest(
    @Json(name = "owner_id") val ownerId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "address") val address: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "monthly_rent") val monthlyRent: Double,
    @Json(name = "amenities") val amenities: List<String>,
    @Json(name = "rules") val rules: String,
    @Json(name = "image_url") val imageUrl: String
)

@JsonClass(generateAdapter = true)
data class SendEnquiryRequest(
    @Json(name = "pg_id") val pgId: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class ReplyEnquiryRequest(
    @Json(name = "enquiry_id") val enquiryId: Int,
    @Json(name = "owner_id") val ownerId: Int,
    @Json(name = "owner_reply") val ownerReply: String
)

@JsonClass(generateAdapter = true)
data class UpdateStatusRequest(
    @Json(name = "pg_id") val pgId: Int,
    @Json(name = "status") val status: String,
    @Json(name = "admin_id") val adminId: Int = 1
)

@JsonClass(generateAdapter = true)
data class UploadData(
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "file_name") val fileName: String? = null
)

@JsonClass(generateAdapter = true)
data class ToggleOccupancyRequest(
    @Json(name = "pg_id") val pgId: Int,
    @Json(name = "owner_id") val ownerId: Int,
    @Json(name = "is_occupied") val isOccupied: Int
)

