package com.example.gigwork.presentation.viewmodels

import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MobileVerificationState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val isCodeSent: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val verificationId: String? = null
)

@HiltViewModel
class MobileVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle,
    private val connectivityManager: ConnectivityManager,
    private val logger: Logger
) : ViewModel() {

    companion object {
        private const val TAG = "MobileVerificationViewModel"
        private const val KEY_USER_TYPE = "userType"
        private const val KEY_PHONE_NUMBER = "phoneNumber"
    }

    val _uiState = MutableStateFlow(MobileVerificationState())
    val uiState = _uiState.asStateFlow()

    val _events = MutableSharedFlow<VerificationEvent>()
    val events = _events.asSharedFlow()

    init {
        // Initialize the user type from savedStateHandle
        savedStateHandle.get<String>(KEY_USER_TYPE)?.let { userType ->
            // Just store the user type, but don't automatically send verification
            logger.d(
                tag = TAG,
                message = "Initialized with user type",
                additionalData = mapOf("userType" to userType)
            )
        }
    }

    fun getUserType(): String {
        return savedStateHandle.get<String>(KEY_USER_TYPE) ?: UserType.EMPLOYEE.name
    }

    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Validate phone number format
     */
    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = "^\\+[1-9]\\d{1,14}$"
        return phoneNumber.matches(phoneRegex.toRegex())
    }

    /**
     * Send verification code to phone number
     */
    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity? = null
    ) {
        // Validate phone number format
        if (!validatePhoneNumber(phoneNumber)) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = ErrorMessage(
                        message = "Invalid phone number format. Please use international format (e.g., +1234567890).",
                        title = "Validation Error"
                    )
                )
            }
            return
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = ErrorMessage(
                        message = "No internet connection. Please check your network settings and try again.",
                        title = "Network Error"
                    )
                )
            }
            return
        }

        // Check if activity is provided
        if (activity == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = ErrorMessage(
                        message = "An active screen is required to send verification code",
                        title = "Verification Error"
                    )
                )
            }
            return
        }

        // Save phone number for potential resend
        savedStateHandle[KEY_PHONE_NUMBER] = phoneNumber

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                logger.d(
                    tag = TAG,
                    message = "Sending verification code",
                    additionalData = mapOf("phone" to phoneNumber)
                )

                authRepository.sendOtpToPhone(
                    phoneNumber = phoneNumber,
                    activity = activity
                ).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            logger.i(
                                tag = TAG,
                                message = "Verification code sent successfully",
                                additionalData = mapOf(
                                    "phone" to phoneNumber,
                                    "verification_id" to apiResult.data.take(5) + "..."
                                )
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isCodeSent = true,
                                    verificationId = apiResult.data
                                )
                            }
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to send verification code",
                                throwable = apiResult.error,
                                additionalData = mapOf("phone" to phoneNumber)
                            )
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = apiResult.error.message ?: "Failed to send verification code",
                                        title = "Verification Error"
                                    )
                                )
                            }
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Exception while sending verification code",
                    throwable = e,
                    additionalData = mapOf("phone" to phoneNumber)
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "Unknown error occurred",
                            title = "Verification Error"
                        )
                    )
                }
            }
        }
    }

    /**
     * Verify OTP code
     */
    fun verifyCode(code: String) {
        // Check if code is valid
        if (code.length != 6 || !code.all { it.isDigit() }) {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "OTP must be exactly 6 digits",
                        title = "Validation Error"
                    )
                )
            }
            return
        }

        val verificationId = _uiState.value.verificationId

        if (verificationId.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "Invalid verification session",
                        title = "Verification Error"
                    )
                )
            }
            return
        }

        // Check network connectivity
        if (!isNetworkAvailable()) {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "No internet connection. Please check your network settings and try again.",
                        title = "Network Error"
                    )
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                logger.d(
                    tag = TAG,
                    message = "Verifying code",
                    additionalData = mapOf(
                        "verification_id" to verificationId.take(5) + "..."
                    )
                )

                authRepository.verifyOtp(verificationId, code).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            val authState = apiResult.data

                            logger.i(
                                tag = TAG,
                                message = "Code verification successful",
                                additionalData = mapOf(
                                    "user_id" to authState.userId,
                                    "user_type" to authState.userType
                                )
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isVerified = true
                                )
                            }

                            // Check if user exists in the system
                            checkUserInDatabase(authState.userId, authState.userType)
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Code verification failed",
                                throwable = apiResult.error
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = apiResult.error.message ?: "Failed to verify code",
                                        title = "Verification Error"
                                    )
                                )
                            }
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Exception while verifying code",
                    throwable = e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "Unknown error occurred",
                            title = "Verification Error"
                        )
                    )
                }
            }
        }
    }

    /**
     * Resend verification code
     */
    fun resendVerificationCode(activity: Activity? = null) {
        // Check if activity is provided
        if (activity == null) {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "An active screen is required to resend verification code",
                        title = "Verification Error"
                    )
                )
            }
            return
        }

        savedStateHandle.get<String>(KEY_PHONE_NUMBER)?.let { phoneNumber ->
            sendVerificationCode(phoneNumber, activity)
        } ?: run {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "Phone number not available. Please go back and try again.",
                        title = "Verification Error"
                    )
                )
            }
        }
    }

    /**
     * Check if user exists in database
     */
    private fun checkUserInDatabase(userId: String, userType: String) {
        viewModelScope.launch {
            try {
                if (!isNetworkAvailable()) {
                    _events.emit(VerificationEvent.NavigateToProfileSetup(userId, userType))
                    return@launch
                }

                userRepository.userExists(userId).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            val userExists = apiResult.data

                            if (userExists) {
                                // User exists - navigate to appropriate screen based on user type
                                when (userType) {
                                    UserType.EMPLOYEE.name -> {
                                        _events.emit(VerificationEvent.NavigateToJobsScreen)
                                    }
                                    UserType.EMPLOYER.name -> {
                                        _events.emit(VerificationEvent.NavigateToEmployerDashboard)
                                    }
                                    else -> {
                                        _events.emit(VerificationEvent.NavigateToJobsScreen)
                                    }
                                }
                            } else {
                                // New user - navigate to profile setup
                                _events.emit(VerificationEvent.NavigateToProfileSetup(userId, userType))
                            }
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to check user existence",
                                throwable = apiResult.error,
                                additionalData = mapOf("user_id" to userId)
                            )

                            // Assume new user if we can't determine
                            _events.emit(VerificationEvent.NavigateToProfileSetup(userId, userType))
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Exception checking user existence",
                    throwable = e,
                    additionalData = mapOf("user_id" to userId)
                )

                // Assume new user if exception occurs
                _events.emit(VerificationEvent.NavigateToProfileSetup(userId, userType))
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

sealed class VerificationEvent {
    data class NavigateToProfileSetup(val userId: String, val userType: String) : VerificationEvent()
    object NavigateToJobsScreen : VerificationEvent()
    object NavigateToEmployerDashboard : VerificationEvent()
}