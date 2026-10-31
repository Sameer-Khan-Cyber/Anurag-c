package com.example.pglocator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pglocator.data.model.LoginRequest
import com.example.pglocator.data.model.RegisterRequest
import com.example.pglocator.data.model.User
import com.example.pglocator.data.repository.PGRepository
import com.example.pglocator.util.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val repository: PGRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(
            currentUser = sessionManager.getCurrentUser(),
            isLoggedIn = sessionManager.isLoggedIn()
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your email address.")
            return
        }
        if (pass.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(LoginRequest(trimmedEmail, pass))
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isLoggedIn = true,
                        errorMessage = null,
                        successMessage = "Welcome back, ${user.fullName}!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Login failed. Check credentials."
                    )
                }
            )
        }
    }

    fun register(fullName: String, email: String, phone: String, pass: String, role: String) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()

        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your full name.")
            return
        }
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }
        if (trimmedPhone.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter your contact phone number.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.register(RegisterRequest(trimmedName, trimmedEmail, trimmedPhone, pass, role))
            result.fold(
                onSuccess = { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isLoggedIn = true,
                        errorMessage = null,
                        successMessage = "Account created successfully!"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Registration failed."
                    )
                }
            )
        }
    }

    fun switchRole(newRole: String) {
        sessionManager.setRole(newRole)
        val user = _uiState.value.currentUser?.copy(role = newRole)
        if (user != null) {
            sessionManager.saveUserSession(user)
        }
        _uiState.value = _uiState.value.copy(currentUser = user)
    }

    fun logout() {
        sessionManager.clearSession()
        _uiState.value = AuthUiState(currentUser = null, isLoggedIn = false)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
