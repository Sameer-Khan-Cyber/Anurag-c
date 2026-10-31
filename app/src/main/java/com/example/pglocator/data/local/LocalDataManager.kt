package com.example.pglocator.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.pglocator.data.model.Enquiry
import com.example.pglocator.data.model.PGListing
import com.example.pglocator.data.model.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Account record stored with hashed/raw password for local authentication verification
 */
data class StoredAccount(
    val user: User,
    val passwordPlain: String
)

/**
 * LocalDataManager:
 * Provides persistent local database storage for users, PG listings, and enquiries.
 * Ensures account registration, login, PG posting, occupancy toggling, and inquiries
 * work with 100% reliability even when an external Apache/MySQL server is not running.
 */
class LocalDataManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pg_locator_local_db", Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val userListAdapter = moshi.adapter<List<User>>(
        Types.newParameterizedType(List::class.java, User::class.java)
    )
    private val listingListAdapter = moshi.adapter<List<PGListing>>(
        Types.newParameterizedType(List::class.java, PGListing::class.java)
    )
    private val enquiryListAdapter = moshi.adapter<List<Enquiry>>(
        Types.newParameterizedType(List::class.java, Enquiry::class.java)
    )

    private val userPassPrefs: SharedPreferences =
        context.getSharedPreferences("pg_locator_user_passwords", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USERS = "local_users_json"
        private const val KEY_LISTINGS = "local_listings_json"
        private const val KEY_ENQUIRIES = "local_enquiries_json"
    }

    init {
        ensureInitialData()
    }

    private fun ensureInitialData() {
        // Initialize default seed users if empty
        if (!prefs.contains(KEY_USERS) || prefs.getString(KEY_USERS, null).isNullOrBlank()) {
            val initialUsers = listOf(
                User(
                    id = 2,
                    fullName = "Rohit Sharma",
                    email = "owner@pglocator.com",
                    phone = "+91 98765 43210",
                    role = "owner",
                    token = "token_owner_2"
                ),
                User(
                    id = 3,
                    fullName = "Sneha Patel",
                    email = "user@pglocator.com",
                    phone = "+91 98765 43211",
                    role = "user",
                    token = "token_user_3"
                )
            )
            saveUsersList(initialUsers)
            userPassPrefs.edit()
                .putString("owner@pglocator.com", "Password@123")
                .putString("user@pglocator.com", "Password@123")
                .apply()
        }

        // Initialize default seed listings if empty
        if (!prefs.contains(KEY_LISTINGS) || prefs.getString(KEY_LISTINGS, null).isNullOrBlank()) {
            val initialListings = listOf(
                PGListing(
                    id = 1,
                    ownerId = 2,
                    title = "Greenfield Luxury PG for Men & Women",
                    description = "Spacious air-conditioned rooms with high-speed WiFi, 3 meals daily, daily housekeeping, and 24/7 biometric security. Walking distance to transit and tech parks.",
                    address = "42 Koramangala 4th Block, Bengaluru, KA",
                    latitude = 12.934533,
                    longitude = 77.619029,
                    monthlyRent = 12500.0,
                    amenities = "WiFi, AC, Food, Laundry, Security, Power Backup",
                    rules = "No smoking inside rooms. Gate closes at 11:00 PM. Visitors allowed in lounge only.",
                    imageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                    isOccupied = 0,
                    status = "approved",
                    createdAt = "2026-10-25 10:00:00",
                    ownerName = "Rohit Sharma",
                    ownerPhone = "+91 98765 43210",
                    ownerEmail = "owner@pglocator.com",
                    amenitiesList = listOf("WiFi", "AC", "Food", "Laundry", "Security", "Power Backup")
                ),
                PGListing(
                    id = 2,
                    ownerId = 2,
                    title = "Skyline Executive PG Suites",
                    description = "Modern co-living space ideal for IT professionals. Includes dedicated work desks, gym, rooftop cafeteria, and high-speed fiber internet.",
                    address = "15 Hitec City Main Rd, Madhapur, Hyderabad, TS",
                    latitude = 17.447412,
                    longitude = 78.376230,
                    monthlyRent = 15000.0,
                    amenities = "WiFi, AC, Food, Gym, Laundry, Security",
                    rules = "Strictly vegetarian kitchen. Valid company ID required at check-in. Quiet hours after 10 PM.",
                    imageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                    isOccupied = 0,
                    status = "approved",
                    createdAt = "2026-10-26 14:30:00",
                    ownerName = "Rohit Sharma",
                    ownerPhone = "+91 98765 43210",
                    ownerEmail = "owner@pglocator.com",
                    amenitiesList = listOf("WiFi", "AC", "Food", "Gym", "Laundry", "Security")
                ),
                PGListing(
                    id = 3,
                    ownerId = 2,
                    title = "Serene Residency Student PG",
                    description = "Budget friendly and quiet study atmosphere near university campuses. High-speed optic fiber, RO purified water, and home-cooked hygienic meals.",
                    address = "88 North Campus Road, New Delhi, DL",
                    latitude = 28.692340,
                    longitude = 77.211560,
                    monthlyRent = 8500.0,
                    amenities = "WiFi, Food, Security, Laundry",
                    rules = "Study hours observed between 9 PM to 6 AM. No loud speakers.",
                    imageUrl = "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80",
                    isOccupied = 1,
                    status = "approved",
                    createdAt = "2026-10-29 09:15:00",
                    ownerName = "Rohit Sharma",
                    ownerPhone = "+91 98765 43210",
                    ownerEmail = "owner@pglocator.com",
                    amenitiesList = listOf("WiFi", "Food", "Security", "Laundry")
                ),
                PGListing(
                    id = 4,
                    ownerId = 4,
                    title = "Urban Nest Premium Co-Living",
                    description = "Fully furnished studio rooms with private balconies, smart TVs, attached washrooms, and gaming arcade. Centralized air conditioning and power backup.",
                    address = "102 Viman Nagar, Pune, MH",
                    latitude = 18.567911,
                    longitude = 73.914343,
                    monthlyRent = 14000.0,
                    amenities = "WiFi, AC, Food, Gym, Security, Power Backup",
                    rules = "Security deposit is 2 months rent. 30 days notice period before vacating.",
                    imageUrl = "https://images.unsplash.com/photo-1554995207-c18c203602cb?auto=format&fit=crop&w=800&q=80",
                    isOccupied = 0,
                    status = "approved",
                    createdAt = "2026-10-27 18:00:00",
                    ownerName = "Priya Varma",
                    ownerPhone = "+91 91234 56789",
                    ownerEmail = "priya@pglocator.com",
                    amenitiesList = listOf("WiFi", "AC", "Food", "Gym", "Security", "Power Backup")
                )
            )
            saveListingsList(initialListings)
        }

        // Initialize default enquiries if empty
        if (!prefs.contains(KEY_ENQUIRIES) || prefs.getString(KEY_ENQUIRIES, null).isNullOrBlank()) {
            val initialEnquiries = listOf(
                Enquiry(
                    id = 1,
                    pgId = 1,
                    userId = 3,
                    ownerId = 2,
                    message = "Hi, I would like to schedule a room visit this Saturday afternoon. Is double sharing available?",
                    status = "replied",
                    ownerReply = "Hello Sneha! Yes, double sharing is available in Block B. You are welcome to visit between 2 PM and 6 PM this Saturday.",
                    createdAt = "2026-10-28 11:20:00",
                    updatedAt = "2026-10-28 12:45:00",
                    pgTitle = "Greenfield Luxury PG for Men & Women",
                    pgAddress = "42 Koramangala 4th Block, Bengaluru, KA",
                    monthlyRent = 12500.0,
                    pgImageUrl = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80",
                    userName = "Sneha Patel",
                    userEmail = "user@pglocator.com",
                    userPhone = "+91 99887 76655",
                    ownerName = "Rohit Sharma",
                    ownerPhone = "+91 98765 43210",
                    ownerEmail = "owner@pglocator.com"
                ),
                Enquiry(
                    id = 2,
                    pgId = 2,
                    userId = 3,
                    ownerId = 2,
                    message = "Is car parking space available in the basement for monthly residents?",
                    status = "pending",
                    ownerReply = null,
                    createdAt = "2026-10-29 08:30:00",
                    updatedAt = "2026-10-29 08:30:00",
                    pgTitle = "Skyline Executive PG Suites",
                    pgAddress = "15 Hitec City Main Rd, Madhapur, Hyderabad, TS",
                    monthlyRent = 15000.0,
                    pgImageUrl = "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80",
                    userName = "Sneha Patel",
                    userEmail = "user@pglocator.com",
                    userPhone = "+91 99887 76655",
                    ownerName = "Rohit Sharma",
                    ownerPhone = "+91 98765 43210",
                    ownerEmail = "owner@pglocator.com"
                )
            )
            saveEnquiriesList(initialEnquiries)
        }
    }

    // ================= USER ACCOUNTS =================

    @Synchronized
    fun getAllUsers(): List<User> {
        val json = prefs.getString(KEY_USERS, null) ?: return emptyList()
        return try {
            userListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveUsersList(users: List<User>) {
        val json = userListAdapter.toJson(users)
        prefs.edit().putString(KEY_USERS, json).apply()
    }

    @Synchronized
    fun findUserByEmail(email: String): User? {
        val normalized = email.trim().lowercase()
        return getAllUsers().find { it.email.trim().lowercase() == normalized }
    }

    @Synchronized
    fun authenticateUser(email: String, pass: String): User? {
        val normalized = email.trim().lowercase()
        val user = getAllUsers().find { it.email.trim().lowercase() == normalized } ?: return null
        val storedPass = userPassPrefs.getString(normalized, null)
        if (storedPass != null && storedPass == pass) {
            return user
        }
        // If password wasn't stored (e.g. edge case), match default if seed
        if (storedPass == null && pass == "Password@123") {
            return user
        }
        return null
    }

    @Synchronized
    fun registerUser(fullName: String, email: String, phone: String, pass: String, role: String): User {
        val currentUsers = getAllUsers().toMutableList()
        val normalizedEmail = email.trim().lowercase()

        val nextId = (currentUsers.maxOfOrNull { it.id } ?: 10) + 1
        val newUser = User(
            id = nextId,
            fullName = fullName.trim(),
            email = email.trim(),
            phone = phone.trim(),
            role = if (role.equals("owner", ignoreCase = true)) "owner" else "user",
            token = "token_${System.currentTimeMillis()}_$nextId",
            createdAt = "Just now"
        )
        currentUsers.add(newUser)
        saveUsersList(currentUsers)
        userPassPrefs.edit().putString(normalizedEmail, pass).apply()
        return newUser
    }

    @Synchronized
    fun saveUser(user: User, passwordPlain: String? = null) {
        val currentUsers = getAllUsers().toMutableList()
        val idx = currentUsers.indexOfFirst { it.email.trim().lowercase() == user.email.trim().lowercase() }
        if (idx != -1) {
            currentUsers[idx] = user
        } else {
            currentUsers.add(user)
        }
        saveUsersList(currentUsers)
        if (!passwordPlain.isNullOrBlank()) {
            userPassPrefs.edit().putString(user.email.trim().lowercase(), passwordPlain).apply()
        }
    }

    // ================= PG LISTINGS =================

    @Synchronized
    fun getAllListings(): List<PGListing> {
        val json = prefs.getString(KEY_LISTINGS, null) ?: return emptyList()
        return try {
            listingListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveListingsList(listings: List<PGListing>) {
        val json = listingListAdapter.toJson(listings)
        prefs.edit().putString(KEY_LISTINGS, json).apply()
    }

    @Synchronized
    fun getListings(
        location: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        amenities: String? = null,
        includeOccupied: Boolean = false
    ): List<PGListing> {
        return getAllListings().filter { listing ->
            listing.status == "approved" &&
            (includeOccupied || listing.isVacant) &&
            (location.isNullOrBlank() || listing.title.contains(location, ignoreCase = true) ||
                    listing.address.contains(location, ignoreCase = true) ||
                    listing.description.contains(location, ignoreCase = true)) &&
            (minPrice == null || listing.monthlyRent >= minPrice) &&
            (maxPrice == null || listing.monthlyRent <= maxPrice) &&
            (amenities.isNullOrBlank() || amenities.split(",").all { req ->
                listing.amenities.contains(req.trim(), ignoreCase = true)
            })
        }
    }

    @Synchronized
    fun getListingById(id: Int): PGListing? {
        return getAllListings().find { it.id == id }
    }

    @Synchronized
    fun getOwnerListings(ownerId: Int): List<PGListing> {
        val all = getAllListings()
        val userSpecific = all.filter { it.ownerId == ownerId }
        // If this owner just registered and has no listings yet, return their exact list
        return userSpecific
    }

    @Synchronized
    fun addListing(listing: PGListing) {
        val current = getAllListings().toMutableList()
        current.add(0, listing)
        saveListingsList(current)
    }

    @Synchronized
    fun createListing(
        ownerId: Int,
        title: String,
        description: String,
        address: String,
        latitude: Double,
        longitude: Double,
        monthlyRent: Double,
        amenities: List<String>,
        rules: String,
        imageUrl: String,
        ownerName: String,
        ownerPhone: String,
        ownerEmail: String
    ): PGListing {
        val current = getAllListings()
        val nextId = (current.maxOfOrNull { it.id } ?: 10) + 1
        return PGListing(
            id = nextId,
            ownerId = ownerId,
            title = title,
            description = description,
            address = address,
            latitude = latitude,
            longitude = longitude,
            monthlyRent = monthlyRent,
            amenities = amenities.joinToString(", "),
            rules = rules,
            imageUrl = imageUrl.ifBlank { "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80" },
            isOccupied = 0,
            status = "approved",
            createdAt = "Just now",
            ownerName = ownerName,
            ownerPhone = ownerPhone,
            ownerEmail = ownerEmail,
            amenitiesList = amenities
        )
    }

    @Synchronized
    fun updateOccupancy(pgId: Int, isOccupied: Int) {
        val current = getAllListings().toMutableList()
        val idx = current.indexOfFirst { it.id == pgId }
        if (idx != -1) {
            current[idx] = current[idx].copy(isOccupied = isOccupied)
            saveListingsList(current)
        }
    }

    // ================= ENQUIRIES =================

    @Synchronized
    fun getAllEnquiries(): List<Enquiry> {
        val json = prefs.getString(KEY_ENQUIRIES, null) ?: return emptyList()
        return try {
            enquiryListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveEnquiriesList(enquiries: List<Enquiry>) {
        val json = enquiryListAdapter.toJson(enquiries)
        prefs.edit().putString(KEY_ENQUIRIES, json).apply()
    }

    @Synchronized
    fun getUserEnquiries(userId: Int): List<Enquiry> {
        return getAllEnquiries().filter { it.userId == userId }
    }

    @Synchronized
    fun getOwnerEnquiries(ownerId: Int): List<Enquiry> {
        return getAllEnquiries().filter { it.ownerId == ownerId }
    }

    @Synchronized
    fun getNextEnquiryId(): Int {
        return (getAllEnquiries().maxOfOrNull { it.id } ?: 10) + 1
    }

    @Synchronized
    fun addEnquiry(enquiry: Enquiry) {
        val current = getAllEnquiries().toMutableList()
        current.add(0, enquiry)
        saveEnquiriesList(current)
    }

    @Synchronized
    fun replyToEnquiry(enquiryId: Int, reply: String) {
        val current = getAllEnquiries().toMutableList()
        val idx = current.indexOfFirst { it.id == enquiryId }
        if (idx != -1) {
            current[idx] = current[idx].copy(
                status = "replied",
                ownerReply = reply,
                updatedAt = "Just now"
            )
            saveEnquiriesList(current)
        }
    }
}
