package com.example.gigwork.presentation.viewmodels

import android.app.Activity
import android.os.CountDownTimer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.errorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.core.result.toAppError
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.domain.models.AuthState
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.domain.usecase.LoginUseCase
import com.example.gigwork.util.Logger
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authState: AuthState? = null,
    val errorMessage: ErrorMessage? = null,
    // For verification
    val isVerificationSuccessful: Boolean = false,
    val verificationId: String? = null,
    val lastPhoneNumber: String? = null,
    // From PhoneAuthViewModel
    val otpCode: String = "",
    val isCodeSent: Boolean = false,
    val cooldownSeconds: Int = 0,
    val resendAttempts: Int = 0,
    val maxResendAttempts: Int = 3,
    val otpExpirationSeconds: Int = 0,
    val autoDetectInProgress: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val errorHandler: GlobalErrorHandler,
    private val savedStateHandle: SavedStateHandle,
    private val encryptedPreferences: EncryptedPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val supabaseClient: com.example.gigwork.data.api.SupabaseClient,  // Use your custom class
    private val loginUseCase: LoginUseCase,  // Add this
    private val logger: Logger

) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
        private const val KEY_USER_TYPE = "userType"
        private const val KEY_VERIFICATION_ID = "verificationId"
        private const val KEY_PHONE_NUMBER = "phoneNumber"
        private const val KEY_PROCESSING_USER_TYPE = "processing_user_type"

        // Constants from PhoneAuthViewModel
        private const val OTP_TIMEOUT_SECONDS = 120L
        private const val OTP_EXPIRATION_SECONDS = 300 // 5 minutes
        private const val RESEND_COOLDOWN_SECONDS = 30
        private const val MAX_RESEND_ATTEMPTS = 3
    }

    private val _uiState = MutableStateFlow(AuthUiState(
        maxResendAttempts = MAX_RESEND_ATTEMPTS
    ))
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events = _events.asSharedFlow()

    // Timers for expiration and cooldown
    private var resendCooldownTimer: CountDownTimer? = null
    private var otpExpirationTimer: CountDownTimer? = null

    init {
        checkAuthState()
    }

    fun clearNavigationEvent() {
        _events.tryEmit(AuthEvent.None) // Reset to dummy event
    }


    // Existing methods for token-based auth
    fun saveAuthData(token: String, userId: String, userType: String) {
        encryptedPreferences.apply {
            saveAuthToken(token)
            saveUserId(userId)
            saveString("user_type", userType)
        }
    }

    fun getAuthData(): AuthState {
        return AuthState(
            token = encryptedPreferences.getAuthToken(),
            userId = encryptedPreferences.getUserId(),
            userType = encryptedPreferences.getString("user_type")
        )
    }



    fun clearAuthData() {
        encryptedPreferences.clearAuthData()
        firebaseAuth.signOut()
    }

    private fun saveAuthenticationDetails(user: User) {
        encryptedPreferences.apply {
            saveUserId(user.id)
            saveString("user_type", user.type.name)
            saveString("phone_number", user.phone ?: "")
        }
    }

    private fun validatePhoneNumber(phoneNumber: String) {
        val phoneRegex = "^\\+[1-9]\\d{1,14}$"
        if (!phoneNumber.matches(phoneRegex.toRegex())) {
            throw AppError.ValidationError(
                message = "Invalid phone number format",
                field = "phoneNumber"
            )
        }
    }



    /**
     * Login user with phone number
     */
    fun login(
        identifier: String,
        password: String,
        isEmployer: Boolean,
        rememberMe: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                loginUseCase(
                    LoginUseCase.Params(
                        identifier = identifier,
                        password = password,
                        isEmployer = isEmployer,
                        rememberMe = rememberMe
                    )
                ).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            // Already set loading state above
                        }
                        is ApiResult.Success -> {
                            val user = result.data
                            logger.i(
                                tag = TAG,
                                message = "Login successful",
                                additionalData = mapOf(
                                    "user_id" to user.id,
                                    "user_type" to user.type.name
                                )
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    // Update any other UI state as needed
                                )
                            }

                            // Determine where to navigate based on user type
                            when (user.type) {
                                UserType.EMPLOYEE -> {
                                    _events.emit(AuthEvent.NavigateToJobsScreen)
                                }
                                UserType.EMPLOYER -> {
                                    _events.emit(AuthEvent.NavigateToEmployerDashboard)
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Login failed",
                                throwable = result.error
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = result.error.message,
                                        title = "Login Error"
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Exception during login",
                    throwable = e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "An unknown error occurred",
                            title = "Login Error"
                        )
                    )
                }
            }
        }
    }


    fun isUserLoggedIn(): Boolean {
        return encryptedPreferences.hasValidAuth() && firebaseAuth.currentUser != null
    }

    fun getUserType(): String {
        return savedStateHandle.get<String>(KEY_USER_TYPE) ?: UserType.EMPLOYEE.name
    }

    /**
     * Save verification data for later use
     */
    fun saveVerificationData(verificationId: String, userType: String) {
        // Save to SavedStateHandle for persistence across process death
        savedStateHandle[KEY_VERIFICATION_ID] = verificationId
        savedStateHandle[KEY_USER_TYPE] = userType

        // Update UI state with verification ID
        _uiState.update { it.copy(
            verificationId = verificationId
        )}
    }


    // Debounce mechanism for navigation events
    private var lastNavigationTime = 0L
    private val NAVIGATION_DEBOUNCE_MS = 500L

    fun setUserType(userType: String) {
        // Check if this is a duplicate request within debounce time
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNavigationTime < NAVIGATION_DEBOUNCE_MS) {
            logger.d(
                tag = TAG,
                message = "Navigation debounced",
                additionalData = mapOf("userType" to userType)
            )
            return
        }

        // Update last navigation time
        lastNavigationTime = currentTime

        // Prevent repeated navigation for the same user type
        if (userType == savedStateHandle.get<String>(KEY_USER_TYPE)) {
            return
        }

        savedStateHandle[KEY_USER_TYPE] = userType

        viewModelScope.launch {
            _events.emit(AuthEvent.NavigateToPhoneEntry(userType))
            try {
                authRepository.saveUserType(UserType.valueOf(userType)).collect { }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Failed to save user type",
                    throwable = e,
                    additionalData = mapOf("userType" to userType)
                )
            }
        }
    }
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                authRepository.getCurrentUser().collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            val authState = apiResult.data

                            if (authState.isValid()) {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = true,
                                        authState = authState
                                    )
                                }

                                // Determine where to navigate based on user type
                                when (authState.userType) {
                                    UserType.EMPLOYEE.name -> {
                                        _events.emit(AuthEvent.NavigateToJobsScreen)
                                    }
                                    UserType.EMPLOYER.name -> {
                                        _events.emit(AuthEvent.NavigateToEmployerDashboard)
                                    }
                                    else -> {
                                        // Default to jobs screen
                                        _events.emit(AuthEvent.NavigateToJobsScreen)
                                    }
                                }
                            } else {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isLoggedIn = false
                                    )
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = false
                                )
                            }
                        }
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "An unknown error occurred",
                            title = "Authentication Error"
                        )
                    )
                }
            }
        }
    }

    fun sendOtpToPhone(
        phoneNumber: String,
        activity: Activity? = null  // Add optional Activity parameter
    ) {
        viewModelScope.launch {
            try {
                // Check cooldown period
                if (uiState.value.cooldownSeconds > 0) {
                    _uiState.update {
                        it.copy(
                            errorMessage = errorMessage {
                                message("Please wait ${uiState.value.cooldownSeconds} seconds before requesting a new code")
                                title("Rate Limited")
                                level(ErrorLevel.WARNING)
                                action(ErrorAction.Dismiss)
                            }
                        )
                    }
                    return@launch
                }

                // Check if max resend attempts reached
                if (uiState.value.resendAttempts >= uiState.value.maxResendAttempts) {
                    _uiState.update {
                        it.copy(
                            errorMessage = errorMessage {
                                message("Maximum verification attempts reached. Please try again later.")
                                title("Too Many Attempts")
                                level(ErrorLevel.WARNING)
                                action(ErrorAction.Dismiss)
                            }
                        )
                    }
                    return@launch
                }

                // Require Activity for phone verification
                if (activity == null) {
                    _uiState.update {
                        it.copy(
                            errorMessage = errorMessage {
                                message("An active screen is required to send verification code")
                                title("Verification Error")
                                level(ErrorLevel.ERROR)
                                action(ErrorAction.Dismiss)
                            }
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                // Format phone number if needed
                val formattedPhone = formatPhoneNumber(phoneNumber)

                // Save phone number for potential use later
                savedStateHandle[KEY_PHONE_NUMBER] = formattedPhone

                logger.d(
                    tag = TAG,
                    message = "Sending OTP to phone",
                    additionalData = mapOf("phone" to formattedPhone)
                )

                // Pass the activity to sendOtpToPhone method
                authRepository.sendOtpToPhone(
                    phoneNumber = phoneNumber,
                    activity = activity
                ).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            val verificationId = apiResult.data

                            logger.i(
                                tag = TAG,
                                message = "OTP sent successfully",
                                additionalData = mapOf(
                                    "verification_id" to verificationId.take(5) + "..."
                                )
                            )

                            // Save verification ID
                            savedStateHandle[KEY_VERIFICATION_ID] = verificationId

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    verificationId = verificationId,
                                    lastPhoneNumber = formattedPhone,
                                    isCodeSent = true,
                                    otpExpirationSeconds = OTP_EXPIRATION_SECONDS,
                                    resendAttempts = it.resendAttempts + 1
                                )
                            }

                            // Start timers
                            startExpirationTimer()
                            startCooldownTimer()

                            // Emit event to navigate to verification screen
                            _events.emit(AuthEvent.NavigateToVerification(formattedPhone, verificationId))
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to send OTP",
                                throwable = apiResult.error,
                                additionalData = mapOf("phone" to formattedPhone)
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = apiResult.error.message,
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
                    message = "Exception sending OTP",
                    throwable = e,
                    additionalData = mapOf("phone" to phoneNumber)
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "An unknown error occurred",
                            title = "Verification Error"
                        )
                    )
                }
            }
        }
    }


    fun updateOtpCode(code: String) {
        if (code.length <= 6 && code.all { it.isDigit() }) {
            _uiState.update { state ->
                state.copy(otpCode = code, errorMessage = null)
            }

            // Auto-verify when code is complete
            if (code.length == 6) {
                verifyCode(code)
            }
        }
    }

    fun verifyCode(code: String) {
        val verificationId = savedStateHandle.get<String>(KEY_VERIFICATION_ID)

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

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null, autoDetectInProgress = false) }

                logger.d(
                    tag = TAG,
                    message = "Verifying OTP code",
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
                                message = "OTP verification successful",
                                additionalData = mapOf(
                                    "user_id" to authState.userId,
                                    "user_type" to authState.userType
                                )
                            )

                            // Cancel all timers
                            cancelTimers()

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    isVerificationSuccessful = true,
                                    authState = authState
                                )
                            }

                            // Check if user exists or needs profile setup
                            checkUserExistence(authState.userId, authState.userType)
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "OTP verification failed",
                                throwable = apiResult.error
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = apiResult.error.message,
                                        title = "Verification Error",
                                        action = ErrorAction.Dismiss,
                                        level = ErrorLevel.WARNING
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
                    message = "Exception verifying OTP",
                    throwable = e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "An unknown error occurred",
                            title = "Verification Error"
                        )
                    )
                }
            }
        }
    }

    fun resendVerificationCode(activity: Activity? = null) {
        // Get the saved phone number
        val phoneNumber = savedStateHandle.get<String>(KEY_PHONE_NUMBER)

        // Reset OTP state
        _uiState.update {
            it.copy(
                otpCode = "",
                isCodeSent = false,
                verificationId = "",
                errorMessage = null
            )
        }

        if (phoneNumber.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = ErrorMessage(
                        message = "Phone number not available. Please try again.",
                        title = "Verification Error"
                    )
                )
            }
            return
        }

        // Resend OTP with activity
        sendOtpToPhone(phoneNumber, activity)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                authRepository.logout().collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = false,
                                    authState = null
                                )
                            }

                            _events.emit(AuthEvent.NavigateToWelcome)
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Logout failed",
                                throwable = apiResult.error
                            )

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = ErrorMessage(
                                        message = apiResult.error.message,
                                        title = "Logout Error"
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
                    message = "Exception during logout",
                    throwable = e
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ErrorMessage(
                            message = e.message ?: "An unknown error occurred",
                            title = "Logout Error"
                        )
                    )
                }
            }
        }
    }

    private fun checkUserExistence(userId: String, userType: String) {
        viewModelScope.launch {
            try {
                userRepository.userExists(userId).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> {
                            val userExists = apiResult.data

                            if (userExists) {
                                // User exists, navigate to appropriate dashboard
                                when (userType) {
                                    UserType.EMPLOYEE.name -> {
                                        _events.emit(AuthEvent.NavigateToJobsScreen)
                                    }
                                    UserType.EMPLOYER.name -> {
                                        _events.emit(AuthEvent.NavigateToEmployerDashboard)
                                    }
                                    else -> {
                                        // Default to jobs screen
                                        _events.emit(AuthEvent.NavigateToJobsScreen)
                                    }
                                }
                            } else {
                                // New user, needs profile setup
                                _events.emit(AuthEvent.NavigateToProfileSetup(userId, userType))
                            }
                        }
                        is ApiResult.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to check user existence",
                                throwable = apiResult.error,
                                additionalData = mapOf("user_id" to userId)
                            )

                            // If we can't determine, assume new user
                            _events.emit(AuthEvent.NavigateToProfileSetup(userId, userType))
                        }
                        is ApiResult.Loading -> {
                            // Already in loading state
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

                // If exception, assume new user
                _events.emit(AuthEvent.NavigateToProfileSetup(userId, userType))
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Custom -> {
                when (action.label) {
                    "Request New Code", "Resend Code" -> resendVerificationCode()
                }
            }
            is ErrorAction.Retry -> {
                // No specific retry action
            }
            else -> clearError()
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimers()
    }
}

sealed class AuthEvent {
    object NavigateToWelcome : AuthEvent()
    object NavigateToJobsScreen : AuthEvent()
    object NavigateToEmployerDashboard : AuthEvent()
    data class NavigateToPhoneEntry(val userType: String) : AuthEvent()
    object CodeSent : AuthEvent()
    data class NavigateToProfileSetup(val userId: String, val userType: String) : AuthEvent()
    data class NavigateToVerification(val phoneNumber: String, val verificationId: String) : AuthEvent()
    object None : AuthEvent() // Added event
}