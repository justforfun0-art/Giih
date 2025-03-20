package com.example.gigwork.presentation.viewmodels

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.util.Logger
import com.example.gigwork.util.ValidationUtils
import com.example.gigwork.util.analytics.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupUiState(
    val name: String = "",
    val phone: String = "",
    val acceptedTerms: Boolean = true,
    val isLoading: Boolean = false,
    val isSignupComplete: Boolean = false,
    val userId: String = "",
    val userType: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val errorMessage: ErrorMessage? = null,
    val validationErrors: Map<String, String> = emptyMap()
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() &&
                phone.isNotBlank() &&
                validationErrors.isEmpty() &&
                acceptedTerms
}

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger,
    private val analytics: Analytics,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "SignupViewModel"
        private const val KEY_USER_TYPE = "userType"
        private const val KEY_FULL_NAME = "full_name"

        // Analytics event names
        private const val EVENT_SIGNUP_ATTEMPT = "signup_attempt"
        private const val EVENT_OTP_SUCCESS = "signup_otp_success"
        private const val EVENT_OTP_FAILURE = "signup_otp_failure"
        private const val EVENT_VALIDATION_ERROR = "signup_validation_error"
        private const val EVENT_USER_TYPE_CHANGED = "signup_user_type_changed"
    }

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState

    private val _events = MutableSharedFlow<SignupEvent>()
    val events: SharedFlow<SignupEvent> = _events.asSharedFlow()

    init {
        restoreState()
    }

    fun onScreenViewed() {
        analytics.trackScreenView("signup_screen", "SignupScreen")
    }

    private fun restoreState() {
        val userType = savedStateHandle.get<String>(KEY_USER_TYPE) ?: ""
        _uiState.update { currentState ->
            currentState.copy(
                name = savedStateHandle.get<String>(KEY_FULL_NAME) ?: "",
                userType = userType
            )
        }
    }

    fun setUserType(userType: String) {
        savedStateHandle[KEY_USER_TYPE] = userType
        _uiState.update { it.copy(userType = userType) }

        // Track user type change
        analytics.trackEvent(
            EVENT_USER_TYPE_CHANGED,
            mapOf("user_type" to userType)
        )
    }

    fun updateName(name: String) {
        savedStateHandle[KEY_FULL_NAME] = name
        _uiState.update {
            it.copy(
                name = name,
                nameError = null,
                validationErrors = it.validationErrors - "name"
            )
        }
        validateName(name)
    }

    fun updatePhone(phone: String) {
        _uiState.update {
            it.copy(
                phone = phone,
                phoneError = null,
                validationErrors = it.validationErrors - "phone"
            )
        }
        validatePhone(phone)
    }

    fun updateTermsAcceptance(accepted: Boolean) {
        _uiState.update { it.copy(acceptedTerms = accepted) }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            _uiState.update {
                it.copy(
                    nameError = "Name is required",
                    validationErrors = it.validationErrors + ("name" to "Name is required")
                )
            }
        } else if (name.length < 3) {
            _uiState.update {
                it.copy(
                    nameError = "Name must be at least 3 characters",
                    validationErrors = it.validationErrors + ("name" to "Name must be at least 3 characters")
                )
            }
        }
    }

    private fun validatePhone(phone: String) {
        if (phone.isBlank()) {
            _uiState.update {
                it.copy(
                    phoneError = "Phone number is required",
                    validationErrors = it.validationErrors + ("phone" to "Phone number is required")
                )
            }
        } else if (phone.length < 10) {
            _uiState.update {
                it.copy(
                    phoneError = "Please enter a valid phone number",
                    validationErrors = it.validationErrors + ("phone" to "Please enter a valid phone number")
                )
            }
        }
    }

    private fun validateAllFields(): Boolean {
        val currentState = _uiState.value
        val validationErrors = mutableMapOf<String, String>()

        // Required fields validation
        if (currentState.name.isBlank()) {
            validationErrors["name"] = "Name is required"
        } else if (currentState.name.length < 3) {
            validationErrors["name"] = "Name must be at least 3 characters"
        }

        if (currentState.phone.isBlank()) {
            validationErrors["phone"] = "Phone number is required"
        } else if (currentState.phone.length < 10) {
            validationErrors["phone"] = "Please enter a valid phone number"
        }

        if (!currentState.acceptedTerms) {
            validationErrors["terms"] = "You must accept the terms and conditions"
        }

        // Merge with existing validation errors
        val mergedErrors = currentState.validationErrors + validationErrors

        _uiState.update {
            it.copy(
                validationErrors = mergedErrors,
                nameError = validationErrors["name"],
                phoneError = validationErrors["phone"]
            )
        }

        // Log validation summary
        logger.i(TAG, "Validation summary: ${validationErrors.size} errors", mapOf(
            "errors" to mergedErrors.toString(),
            "can_submit" to (mergedErrors.isEmpty()).toString()
        ))

        // Track validation errors
        if (validationErrors.isNotEmpty()) {
            analytics.trackEvent(
                EVENT_VALIDATION_ERROR,
                mapOf(
                    "fields" to validationErrors.keys.joinToString(","),
                    "error_count" to validationErrors.size.toString()
                )
            )
        }

        return mergedErrors.isEmpty()
    }

    fun signupWithPhone(name: String, phone: String, acceptedTerms: Boolean) {
        _uiState.update {
            it.copy(
                name = name,
                phone = phone,
                acceptedTerms = acceptedTerms
            )
        }

        signup()
    }

    fun signup() {
        logger.i(TAG, "Signup attempt", mapOf(
            "can_submit" to _uiState.value.canSubmit.toString(),
            "has_errors" to (_uiState.value.validationErrors.isNotEmpty()).toString()
        ))

        if (!validateAllFields()) {
            return
        }

        val currentState = _uiState.value

        // Track signup attempt
        analytics.trackEvent(
            EVENT_SIGNUP_ATTEMPT,
            mapOf(
                "user_type" to currentState.userType
            )
        )

        viewModelScope.launch {
            try {
                // Format the phone number to ensure it has a "+" prefix
                val formattedPhoneNumber = formatPhoneNumber(currentState.phone)

                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                logger.d(
                    tag = TAG,
                    message = "Starting signup process",
                    additionalData = mapOf(
                        "name" to currentState.name,
                        "phone" to maskPhoneNumber(formattedPhoneNumber),
                        "user_type" to currentState.userType
                    )
                )

                // Save user type in AuthRepository
                // Save user type in AuthRepository
                authRepository.saveUserType(
                    userType = if (currentState.userType == "EMPLOYER") UserType.EMPLOYER else UserType.EMPLOYEE
                ).collect { /* ignore result */ }

                // Send OTP with optional activity parameter
                authRepository.sendOtpToPhone(
                    phoneNumber = formattedPhoneNumber,
                    activity = null  // No activity available in this context
                ).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            logger.i(
                                tag = TAG,
                                message = "OTP sent successfully for signup",
                                additionalData = mapOf(
                                    "phone" to maskPhoneNumber(formattedPhoneNumber),
                                    "verification_id" to apiResult.data.take(5) + "..."
                                )
                            )

                            // Track OTP success
                            analytics.trackEvent(
                                EVENT_OTP_SUCCESS,
                                mapOf("phone_hash" to formattedPhoneNumber.hashCode().toString())
                            )

                            _uiState.update { it.copy(isLoading = false) }
                            _events.emit(SignupEvent.OtpSent(apiResult.data))
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to send OTP for signup",
                                throwable = apiResult.error,
                                additionalData = mapOf("phone" to maskPhoneNumber(formattedPhoneNumber))
                            )

                            // Track OTP failure
                            analytics.trackEvent(
                                EVENT_OTP_FAILURE,
                                mapOf(
                                    "error_type" to apiResult.error::class.simpleName,
                                    "error_message" to apiResult.error.message
                                )
                            )

                            handleError(apiResult.error)
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Unexpected error during signup",
                    throwable = e
                )

                // Track exception
                analytics.logError(
                    e,
                    mapOf("screen" to "signup", "action" to "send_otp")
                )

                handleError(e.toAppError())
            }
        }
    }

    private fun formatPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.startsWith("+")) {
            phoneNumber
        } else {
            "+$phoneNumber"
        }
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length <= 4) return "****"

        val visiblePart = phoneNumber.takeLast(4)
        val maskedPart = "*".repeat(phoneNumber.length - 4)
        return maskedPart + visiblePart
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in SignupViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "name" to _uiState.value.name,
                "phone" to maskPhoneNumber(_uiState.value.phone),
                "user_type" to _uiState.value.userType,
                "error_type" to error::class.simpleName
            )
        )

        // Also log the error to analytics
        analytics.logError(
            error,
            mapOf(
                "screen" to "signup",
                "error_type" to error::class.simpleName
            )
        )

        val errorMessage = errorHandler.handleCoreError(error)
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

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> signup()
            is ErrorAction.Custom -> {
                logger.d(TAG, "Custom action: ${action.label}")
            }
            is ErrorAction.Multiple -> {
                when (action.primary) {
                    is ErrorAction.Retry -> signup()
                    else -> clearError()
                }
            }
            else -> clearError()
        }
    }

    private fun Exception.toAppError(): AppError {
        return when (this) {
            is AppError -> this
            else -> AppError.UnexpectedError(
                message = this.message ?: "An unexpected error occurred",
                cause = this,
                errorCode = "SIGNUP_001"
            )
        }
    }

    sealed class SignupEvent {
        data class OtpSent(val verificationId: String) : SignupEvent()
    }
}