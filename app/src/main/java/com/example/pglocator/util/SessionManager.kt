package com.example.pglocator.util

import android.content.Context
import android.content.SharedPreferences
import com.example.pglocator.data.model.User
import com.example.pglocator.data.network.ApiClient

class SessionManager(val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pg_locator_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_ROLE = "role"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_BASE_URL = "base_url"
    }

    init {
        val savedUrl = getBaseUrl()
        ApiClient.updateBaseUrl(savedUrl)
    }

    fun saveUserSession(user: User) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putInt(KEY_USER_ID, user.id)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_EMAIL, user.email)
            putString(KEY_PHONE, user.phone)
            putString(KEY_ROLE, user.role)
            putString(KEY_AUTH_TOKEN, user.token ?: "")
            apply()
        }
    }

    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null
        return User(
            id = prefs.getInt(KEY_USER_ID, 0),
            fullName = prefs.getString(KEY_FULL_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            phone = prefs.getString(KEY_PHONE, "") ?: "",
            role = prefs.getString(KEY_ROLE, "user") ?: "user",
            token = prefs.getString(KEY_AUTH_TOKEN, null)
        )
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getRole(): String = prefs.getString(KEY_ROLE, "user") ?: "user"

    fun setRole(role: String) {
        prefs.edit().putString(KEY_ROLE, role).apply()
    }

    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_USER_ID)
            remove(KEY_FULL_NAME)
            remove(KEY_EMAIL)
            remove(KEY_PHONE)
            remove(KEY_ROLE)
            remove(KEY_AUTH_TOKEN)
            apply()
        }
    }

    fun getBaseUrl(): String = prefs.getString(KEY_BASE_URL, ApiClient.DEFAULT_BASE_URL) ?: ApiClient.DEFAULT_BASE_URL

    fun setBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url).apply()
        ApiClient.updateBaseUrl(url)
    }
}
