package com.example.gigwork.presentation.viewmodels

import android.app.Activity
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.errorMessage
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.AuthRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class PhoneAuthUiState(
    val phoneNumber: String = "",
    val formattedPhoneNumber: String = "",
    val otpCode: String = "",
    val verificationId: String = "",
    val isLoading: Boolean = false,
    val isCodeSent: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: ErrorMessage? = null,
    val cooldownSeconds: Int = 0,
    val resendAttempts: Int = 0,
    val maxResendAttempts: Int = 3,
    val otpExpirationSeconds: Int = 0,
    val selectedUserType: UserType = UserType.EMPLOYEE,
    val autoDetectInProgress: Boolean = false
)

sealed class PhoneAuthResult {
    data class CodeSent(val verificationId: String) : PhoneAuthResult()
    data class Success(val credential: PhoneAuthCredential) : PhoneAuthResult()
    data class Error(val message: String) : PhoneAuthResult()
    object InvalidPhone : PhoneAuthResult()
    object PhoneInUse : PhoneAuthResult()
    object TooManyRequests : PhoneAuthResult()
}

@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Constants for OTP configuration
    companion object {
        private const val OTP_TIMEOUT_SECONDS = 120L
        private const val OTP_EXPIRATION_SECONDS = 300 // 5 minutes
        private const val RESEND_COOLDOWN_SECONDS = 30
        private const val MAX_RESEND_ATTEMPTS = 3
    }

    private val _uiState = MutableStateFlow(PhoneAuthUiState(
        maxResendAttempts = MAX_RESEND_ATTEMPTS
    ))
    val uiState: StateFlow<PhoneAuthUiState> = _uiState

    private var resendCooldownTimer: CountDownTimer? = null
    private var otpExpirationTimer: CountDownTimer? = null
    private var storedVerificationId: String? = null
    private var pendingCredential: PhoneAuthCredential? = null

    // Update phone number
    fun updatePhoneNumber(phoneNumber: String) {
        _uiState.update { it.copy(
            phoneNumber = phoneNumber,
            formattedPhoneNumber = formatPhoneNumber(phoneNumber),
            errorMessage = null
        )}
    }

    // Format phone number for display
    private fun formatPhoneNumber(phoneNumber: String): String {
        // Add international format if not present
        return if (!phoneNumber.startsWith("+")) {
            "+91$phoneNumber" // Default to India code for GigWork
        } else {
            phoneNumber
        }
    }

    // Update OTP code
    fun updateOtpCode(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _uiState.update { it.copy(
                otpCode = code,
                errorMessage = null
            )}

            // Auto-verify if code reaches 6 digits
            if (code.length == 6) {
                verifyOtpCode()
            }
        }
    }

    // Select user type
    fun selectUserType(userType: UserType) {
        _uiState.update { it.copy(selectedUserType = userType) }
    }

    // Request phone verification
    fun requestPhoneVerification(activity: Activity) {
        val phoneNumber = uiState.value.formattedPhoneNumber

        if (phoneNumber.isBlank() || phoneNumber.length < 10) {
            _uiState.update { it.copy(
                errorMessage = errorMessage {
                    message("Please enter a valid phone number")
                    title("Validation Error")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            )}
            return
        }

        // Check if in cooldown period
        if (uiState.value.cooldownSeconds > 0) {
            _uiState.update { it.copy(
                errorMessage = errorMessage {
                    message("Please wait ${uiState.value.cooldownSeconds} seconds before requesting a new code")
                    title("Rate Limited")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            )}
            return
        }

        // Check if max resend attempts reached
        if (uiState.value.resendAttempts >= uiState.value.maxResendAttempts) {
            _uiState.update { it.copy(
                errorMessage = errorMessage {
                    message("Maximum verification attempts reached. Please try again later.")
                    title("Too Many Attempts")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            )}
            return
        }

        _uiState.update { it.copy(
            isLoading = true,
            errorMessage = null
        )}

        // Setup Firebase Phone Auth callbacks
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                pendingCredential = credential
                _uiState.update { it.copy(
                    isLoading = false,
                    autoDetectInProgress = true
                )}
                verifyWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _uiState.update { it.copy(isLoading = false) }

                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        _uiState.update { it.copy(
                            errorMessage = errorMessage {
                                message("Invalid phone number format. Please check and try again.")
                                title("Invalid Phone")
                                level(ErrorLevel.WARNING)
                                action(ErrorAction.Dismiss)
                            }
                        )}
                    }
                    is FirebaseTooManyRequestsException -> {
                        _uiState.update { it.copy(
                            errorMessage = errorMessage {
                                message("Too many requests from this device. Please try again later.")
                                title("Rate Limited")
                                level(ErrorLevel.WARNING)
                                action(ErrorAction.Dismiss)
                            }
                        )}
                    }
                    else -> {
                        _uiState.update { it.copy(
                            errorMessage = errorMessage {
                                message(e.message ?: "An unexpected error occurred")
                                title("Verification Failed")
                                level(ErrorLevel.ERROR)
                                action(ErrorAction.Dismiss)
                            }
                        )}
                    }
                }
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Save verification ID
                storedVerificationId = verificationId

                // Update UI state
                _uiState.update { it.copy(
                    isLoading = false,
                    isCodeSent = true,
                    verificationId = verificationId,
                    otpExpirationSeconds = OTP_EXPIRATION_SECONDS,
                    resendAttempts = it.resendAttempts + 1,
                    errorMessage = null
                )}

                // Start expiration timer
                startExpirationTimer()

                // Start cooldown timer for resend
                startCooldownTimer()
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                // This is called when auto-detection times out
                if (_uiState.value.autoDetectInProgress) {
                    _uiState.update { it.copy(
                        autoDetectInProgress = false,
                        errorMessage = errorMessage {
                            message("Automatic code detection timed out. Please enter the code manually.")
                            title("Manual Entry Required")
                            level(ErrorLevel.INFO)
                            action(ErrorAction.Dismiss)
                        }
                    )}
                }
            }
        }

        // Configure and start phone verification
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    // Verify OTP code
    fun verifyOtpCode() {
        val currentState = uiState.value
        val code = currentState.otpCode
        val verificationId = currentState.verificationId.ifEmpty { storedVerificationId }

        // Validation
        if (code.length != 6) {
            _uiState.update { it.copy(
                errorMessage = errorMessage {
                    message("Please enter a valid 6-digit code")
                    title("Validation Error")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            )}
            return
        }

        if (verificationId.isNullOrBlank()) {
            _uiState.update { it.copy(
                errorMessage = errorMessage {
                    message("Verification session expired or not found")
                    title("Session Error")
                    level(ErrorLevel.ERROR)
                    action(ErrorAction.Custom("Resend Code"))
                }
            )}
            return
        }

        _uiState.update { it.copy(
            isLoading = true,
            errorMessage = null
        )}

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            verifyWithCredential(credential)
        } catch (e: Exception) {
            _uiState.update { it.copy(
                isLoading = false,
                errorMessage = errorMessage {
                    message("Invalid verification code")
                    title("Verification Error")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            )}
        }
    }

    // Handle credential verification
    private fun verifyWithCredential(credential: PhoneAuthCredential) {
        _uiState.update { it.copy(
            isLoading = true,
            autoDetectInProgress = false,
            errorMessage = null
        )}

        viewModelScope.launch {
            try {
                // Sign in to Firebase with the credential
                val auth = Firebase.auth
                val authResult = auth.signInWithCredential(credential).await()

                if (authResult.user != null) {
                    // Create or link Supabase account
                    authRepository.createOrLinkSupabaseAccount(
                        authResult.user!!,
                        uiState.value.selectedUserType
                    )

                    // Update UI state on success
                    _uiState.update { it.copy(
                        isLoading = false,
                        isVerified = true,
                        errorMessage = null
                    )}

                    // Cancel timers
                    cancelTimers()
                } else {
                    throw Exception("Authentication failed: user is null")
                }
            } catch (e: Exception) {
                handleVerificationError(e)
            }
        }
    }

    private fun handleVerificationError(e: Exception) {
        val errorMsg = when (e) {
            is FirebaseAuthInvalidCredentialsException ->
                errorMessage {
                    message("Invalid verification code. Please check and try again.")
                    title("Invalid Code")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            is FirebaseTooManyRequestsException ->
                errorMessage {
                    message("Too many requests. Please try again later.")
                    title("Rate Limited")
                    level(ErrorLevel.WARNING)
                    action(ErrorAction.Dismiss)
                }
            else ->
                errorMessage {
                    message(e.message ?: "An unknown error occurred")
                    title("Verification Error")
                    level(ErrorLevel.ERROR)
                    action(ErrorAction.Dismiss)
                }
        }

        _uiState.update { it.copy(
            isLoading = false,
            errorMessage = errorMsg
        )}
    }

    // Resend OTP
    fun resendCode(activity: Activity) {
        // Update attempt count and request new code
        _uiState.update {
            it.copy(
                otpCode = "",
                isCodeSent = false,
                verificationId = "",
                errorMessage = null
            )
        }

        requestPhoneVerification(activity)
    }

    // Start the expiration timer for OTP
    private fun startExpirationTimer() {
        // Cancel any existing timer
        otpExpirationTimer?.cancel()

        // Create new timer
        otpExpirationTimer = object : CountDownTimer(
            OTP_EXPIRATION_SECONDS * 1000L,
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                _uiState.update { it.copy(otpExpirationSeconds = secondsRemaining.toInt()) }
            }

            override fun onFinish() {
                _uiState.update {
                    it.copy(
                        otpExpirationSeconds = 0,
                        errorMessage = errorMessage {
                            message("Verification code has expired. Please request a new one.")
                            title("Code Expired")
                            level(ErrorLevel.WARNING)
                            action(ErrorAction.Custom("Request New Code"))
                        }
                    )
                }
            }
        }.start()
    }

    // Start cooldown timer for resend
    private fun startCooldownTimer() {
        // Cancel any existing timer
        resendCooldownTimer?.cancel()

        // Set initial cooldown
        _uiState.update { it.copy(cooldownSeconds = RESEND_COOLDOWN_SECONDS) }

        // Create new timer
        resendCooldownTimer = object : CountDownTimer(
            RESEND_COOLDOWN_SECONDS * 1000L,
            1000
        ) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                _uiState.update { it.copy(cooldownSeconds = secondsRemaining.toInt()) }
            }

            override fun onFinish() {
                _uiState.update { it.copy(cooldownSeconds = 0) }
            }
        }.start()
    }

    // Cancel all timers
    private fun cancelTimers() {
        otpExpirationTimer?.cancel()
        resendCooldownTimer?.cancel()
    }

    // Clear error
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // Handle error actions
    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Custom -> {
                when (action.label) {
                    "Request New Code", "Resend Code" -> {
                        // Reset state for new code request
                        _uiState.update {
                            it.copy(
                                otpCode = "",
                                isCodeSent = false,
                                verificationId = "",
                                errorMessage = null
                            )
                        }
                    }
                }
            }
            is ErrorAction.Dismiss -> clearError()
            else -> clearError()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimers()
    }
}