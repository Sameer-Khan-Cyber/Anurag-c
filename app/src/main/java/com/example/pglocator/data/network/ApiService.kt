package com.example.pglocator.data.network

import com.example.pglocator.data.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface ApiService {

    // Auth Endpoints
    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<User>

    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): ApiResponse<User>

    // File Upload Endpoint (Multipart)
    @Multipart
    @POST("upload/upload_image.php")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): ApiResponse<UploadData>

    // Owner Endpoints
    @POST("owner/post_pg.php")
    suspend fun postPG(@Body request: CreatePGRequest): ApiResponse<PGListing>

    @GET("owner/my_pgs.php")
    suspend fun getOwnerPGs(@Query("owner_id") ownerId: Int): ApiResponse<List<PGListing>>

    @GET("owner/enquiries.php")
    suspend fun getOwnerEnquiries(@Query("owner_id") ownerId: Int): ApiResponse<List<Enquiry>>

    @POST("owner/reply_enquiry.php")
    suspend fun replyEnquiry(@Body request: ReplyEnquiryRequest): ApiResponse<Map<String, Any>>

    @POST("owner/toggle_occupancy.php")
    suspend fun toggleOccupancy(@Body request: ToggleOccupancyRequest): ApiResponse<Map<String, Any>>

    // User Endpoints
    @GET("user/search_pg.php")
    suspend fun searchPGs(
        @Query("location") location: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("amenities") amenities: String? = null,
        @Query("include_occupied") includeOccupied: Boolean? = null
    ): ApiResponse<List<PGListing>>

    @POST("user/send_enquiry.php")
    suspend fun sendEnquiry(@Body request: SendEnquiryRequest): ApiResponse<Map<String, Any>>

    @GET("user/my_enquiries.php")
    suspend fun getUserEnquiries(@Query("user_id") userId: Int): ApiResponse<List<Enquiry>>
}
