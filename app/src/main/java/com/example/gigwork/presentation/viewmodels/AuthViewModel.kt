package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.*
import com.example.gigwork.data.repository.AuthRepository
import com.example.gigwork.domain.models.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authState: AuthState? = null,
    val userPreferences: Map<String, String> = emptyMap(),
    val errorMessage: ErrorMessage? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val authState = authRepository.getAuthData()
                val isLoggedIn = authRepository.isUserLoggedIn()
                val preferences = authRepository.getUserPreferences()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = isLoggedIn,
                        authState = authState,
                        userPreferences = preferences
                    )
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun saveAuthData(token: String, userId: String, userType: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                authRepository.saveAuthData(token, userId, userType)
                checkAuthState()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                authRepository.clearAuthData()
                _uiState.value = AuthUiState()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun updateUserPreferences(
        language: String? = null,
        notificationsEnabled: Boolean? = null,
        darkMode: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val currentPrefs = authRepository.getUserPreferences()
                authRepository.saveUserPreferences(
                    language = language ?: currentPrefs["language"] ?: "en",
                    notificationsEnabled = notificationsEnabled
                        ?: currentPrefs["notifications"]?.toBoolean() ?: true,
                    darkMode = darkMode
                        ?: currentPrefs["darkMode"]?.toBoolean() ?: false
                )
                checkAuthState()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(exception: Exception) {
        val errorMessage = when (exception) {
            is AppError.SecurityError -> {
                when (exception.securityDomain) {
                    "authentication" -> errorMessage {
                        message("Session expired. Please login again.")
                        title("Authentication Error")
                        level(ErrorLevel.ERROR)
                        action(ErrorAction.Custom("Login", route = "login_screen"))
                        metadata("error_type", "session_expired")
                    }
                    "authorization" -> errorMessage {
                        message("You don't have permission to perform this action.")
                        title("Authorization Error")
                        level(ErrorLevel.ERROR)
                        action(ErrorAction.Dismiss)
                        metadata("error_type", "unauthorized")
                    }
                    else -> ErrorMessage.criticalError(exception.message)
                }
            }
            is AppError.NetworkError -> ErrorMessage.networkError(
                "Unable to connect to authentication service"
            )
            is AppError.DatabaseError -> errorMessage {
                message("Failed to save authentication data")
                title("Storage Error")
                level(ErrorLevel.ERROR)
                action(ErrorAction.Retry)
                metadata("error_type", "storage_failed")
            }
            else -> ErrorMessage.criticalError(
                exception.message ?: "An unexpected error occurred"
            )
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = errorMessage
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun retry() {
        checkAuthState()
    }
}