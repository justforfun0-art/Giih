package com.example.gigwork.data.repository

import android.app.Activity
import android.content.Context
import android.net.NetworkCapabilities
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.auth.AuthBridgeService
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.domain.models.AuthState
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.util.Logger
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import android.net.ConnectivityManager


class AuthRepositoryImpl @Inject constructor(
    private val encryptedPreferences: EncryptedPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val supabaseClient: SupabaseClient,
    private val authBridgeService: AuthBridgeService,
    private val logger: Logger
) : AuthRepository {

    companion object {
        private const val TAG = "AuthRepositoryImpl"
        private const val OTP_TIMEOUT_SECONDS = 120L
    }

    /**
     * Send OTP verification code to phone number
     */
    /**
     * Send OTP verification code to phone number
     */
    override fun sendOtpToPhone(
        phoneNumber: String,
        activity: Activity?
    ): Flow<ApiResult<String>> = flow {
        emit(ApiResult.loading())

        try {
            // Validate phone number
            validatePhoneNumber(phoneNumber)

            // Check if activity is provided
            if (activity == null) {
                throw AppError.SecurityError(
                    message = "An active screen is required to send verification code",
                    securityDomain = "authentication"
                )
            }

            // Check network connectivity
            if (!isNetworkAvailable(activity)) {
                throw AppError.NetworkError(
                    message = "No internet connection available",
                    isConnectionError = true,
                    errorCode = "NO_INTERNET"
                )
            }

            // Use suspendCancellableCoroutine to properly handle the callbacks
            val verificationId = withContext(Dispatchers.IO) {
                suspendCancellableCoroutine<String> { continuation ->
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            logger.d(TAG, "Auto-verification completed")
                            // We don't resume here as we're waiting for the code to be sent
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            logger.e(TAG, "Verification failed", e)
                            continuation.resumeWithException(e)
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            logger.d(
                                TAG,
                                "Code sent",
                                mapOf("verificationId" to verificationId.take(5) + "...")
                            )
                            continuation.resume(verificationId)
                        }
                    }

                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(OTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(callbacks)
                        .build()

                    try {
                        PhoneAuthProvider.verifyPhoneNumber(options)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }

                    continuation.invokeOnCancellation {
                        // If needed, you can handle cancellation here
                        logger.d(TAG, "OTP sending operation was cancelled")
                    }
                }
            }

            emit(ApiResult.Success(verificationId))

        } catch (e: Exception) {
            logger.e(TAG, "Failed to send OTP", e)
            val appError = when (e) {
                is FirebaseException -> AppError.SecurityError(
                    message = e.message ?: "Firebase authentication error",
                    securityDomain = "authentication"
                )
                is CancellationException -> AppError.NetworkError(
                    message = "OTP verification timed out",
                    isConnectionError = true,
                    errorCode = "OTP_TIMEOUT"
                )
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "An unknown error occurred",
                    cause = e,
                    stackTrace = e.stackTraceToString()
                )
            }
            emit(ApiResult.Error(appError))
        }
    }.flowOn(Dispatchers.IO)

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Validate OTP format
     * @param otp One-time password to validate
     * @throws AppError.ValidationError if OTP is invalid
     */
    private fun validateOtp(otp: String) {
        if (otp.isBlank()) {
            throw AppError.ValidationError(
                message = "OTP cannot be empty",
                field = "otp"
            )
        }

        if (otp.length != 6) {
            throw AppError.ValidationError(
                message = "OTP must be exactly 6 digits",
                field = "otp"
            )
        }

        if (!otp.all { it.isDigit() }) {
            throw AppError.ValidationError(
                message = "OTP must contain only numeric digits",
                field = "otp"
            )
        }
    }


    /**
     * Verify OTP code against the verification ID
     */
    override fun verifyOtp(verificationId: String, otp: String): Flow<ApiResult<AuthState>> = flow {
        emit(ApiResult.loading())

        try {
            // Validate OTP format
            validateOtp(otp)

            // Create credential
            val credential = PhoneAuthProvider.getCredential(verificationId, otp)

            // Sign in with credential
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            // Get user from result
            val firebaseUser = authResult.user ?: throw AppError.SecurityError(
                message = "Authentication failed",
                securityDomain = "authentication"
            )

            // Get user type from preferences
            val userType = encryptedPreferences.getString("user_type", UserType.EMPLOYEE.name)

            // Try to create or update user in Supabase
            try {
                createOrUpdateUserInSupabase(firebaseUser, UserType.valueOf(userType))
            } catch (e: Exception) {
                // Log the error but continue with authentication
                logger.e(TAG, "Failed to sync user with Supabase, continuing with Firebase auth", e)
            }

            // Save authentication details
            saveAuthenticationDetails(firebaseUser, userType)

            // Create and return auth state
            val authState = createAuthState(firebaseUser, userType)

            emit(ApiResult.Success(authState))

        } catch (e: Exception) {
            logger.e(TAG, "Failed to verify OTP", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }.flowOn(Dispatchers.IO)


    /**
     * Save authentication details for Firebase user
     */
    private suspend fun saveAuthenticationDetails(
        firebaseUser: FirebaseUser,
        userType: String
    ) {
        try {
            // Get Firebase ID token
            val token = firebaseUser.getIdToken(false).await().token ?: ""
            // Save authentication details
            encryptedPreferences.apply {
                saveAuthToken(token)
                saveUserId(firebaseUser.uid)
                saveString("phone_number", firebaseUser.phoneNumber ?: "")
                saveString("user_type", userType)
            }
        } catch (e: Exception) {
            logger.e(TAG, "Failed to save authentication details", e)
            throw e
        }
    }
    /**
     * Save user authentication details
     */
    private fun saveUserAuthenticationDetails(user: User) {
        encryptedPreferences.apply {
            saveUserId(user.id)
            saveString("user_type", user.type.name)
            saveString("phone_number", user.phone ?: "")
        }
    }

    /**
     * Create AuthState from Firebase user
     */
    private fun createAuthState(
        firebaseUser: FirebaseUser,
        userType: String
    ): AuthState {
        val token = try {
            Tasks.await(firebaseUser.getIdToken(false)).token ?: ""
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get ID token", e)
            ""
        }

        return AuthState(
            token = token,
            userId = firebaseUser.uid,
            userType = userType
        )
    }


    /**
     * Save user type (employee or employer)
     */
    override fun saveUserType(userType: UserType): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.loading())

        try {
            // Save user type to preferences
            encryptedPreferences.saveString("user_type", userType.name)
            emit(ApiResult.Success(Unit))

        } catch (e: Exception) {
            logger.e(TAG, "Failed to save user type", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get current authenticated user information
     */
    override fun getCurrentUser(): Flow<ApiResult<AuthState>> = flow {
        emit(ApiResult.loading())

        try {
            // Check if user is logged in
            if (!isUserLoggedIn()) {
                emit(ApiResult.Success(AuthState()))
                return@flow
            }

            // Get current user from Firebase
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                emit(ApiResult.Success(AuthState()))
                return@flow
            }

            // Get token
            val token = firebaseUser.getIdToken(false).await().token ?: ""

            // Get user type from preferences
            val userType = encryptedPreferences.getString("user_type", UserType.EMPLOYEE.name)

            // Create and return auth state
            val authState = AuthState(
                token = token,
                userId = firebaseUser.uid,
                userType = userType
            )

            emit(ApiResult.Success(authState))

        } catch (e: Exception) {
            logger.e(TAG, "Failed to get current user", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Logout the current user
     */
    override fun logout(): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.loading())

        try {
            // Sign out from Firebase
            firebaseAuth.signOut()

            // Clear stored authentication data
            clearAuthData()

            emit(ApiResult.Success(Unit))

        } catch (e: Exception) {
            logger.e(TAG, "Failed to logout", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Clear stored authentication data
     */
    override fun clearAuthData() {
        encryptedPreferences.clearAuthData()
    }

    /**
     * Check if user is currently logged in
     */
    override fun isUserLoggedIn(): Boolean {
        return encryptedPreferences.hasValidAuth() && firebaseAuth.currentUser != null
    }

    /**
     * Create or update user in Supabase database
     * Note: This focuses on database operations since Supabase is used for database,
     * not authentication in this application
     */
    override suspend fun createOrLinkSupabaseAccount(
        firebaseUser: FirebaseUser,
        userType: UserType
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        return@withContext createOrUpdateUserInSupabase(firebaseUser, userType)
    }

    /**
     * Helper method to create or update user in Supabase database
     */
    private suspend fun createOrUpdateUserInSupabase(
        firebaseUser: FirebaseUser,
        userType: UserType
    ): ApiResult<Unit> {
        return try {
            // Get phone number
            val phoneNumber = firebaseUser.phoneNumber
                ?: throw AppError.SecurityError(
                    message = "Firebase user does not have a phone number",
                    securityDomain = "authentication"
                )

            try {
                // Try to check if user exists in Supabase database
                val existingUser = supabaseClient.client.postgrest.from("users")
                    .select {
                        filter {
                            eq("firebase_uid", firebaseUser.uid)
                        }
                    }
                    .decodeList<Map<String, Any?>>()

                if (existingUser.isEmpty()) {
                    // Create new user in Supabase database
                    supabaseClient.client.postgrest.from("users")
                        .insert(mapOf(
                            "firebase_uid" to firebaseUser.uid,
                            "phone_number" to phoneNumber,
                            "user_type" to userType.name,
                            "created_at" to System.currentTimeMillis(),
                            "last_login" to System.currentTimeMillis()
                        ))
                } else {
                    // Update existing user
                    supabaseClient.client.postgrest.from("users")
                        .update({
                            set("last_login", System.currentTimeMillis())
                            set("phone_number", phoneNumber)
                            // Only update user_type if it's different
                            if (existingUser.first()["user_type"] != userType.name) {
                                set("user_type", userType.name)
                            }
                        }) {
                            filter { eq("firebase_uid", firebaseUser.uid) }
                        }
                }

            } catch (e: io.github.jan.supabase.exceptions.NotFoundRestException) {
                // Handle "relation does not exist" error
                logger.e(TAG, "Table 'users' does not exist in Supabase", e)

                // Since we can't communicate with Supabase, just save user locally
                // and continue with authentication flow
                encryptedPreferences.apply {
                    saveUserId(firebaseUser.uid)
                    saveString("user_type", userType.name)
                    saveString("phone_number", phoneNumber)
                }
            }

            // Return success even if Supabase operations failed but Firebase auth succeeded
            ApiResult.Success(Unit)

        } catch (e: Exception) {
            logger.e(TAG, "Failed to create or update user in Supabase", e)
            ApiResult.Error(e.toAppError())
        }
    }

    /**
     * Helper method to validate phone number format
     */
    private fun validatePhoneNumber(phoneNumber: String) {
        val phoneRegex = "^\\+[1-9]\\d{1,14}$"
        if (!phoneNumber.matches(phoneRegex.toRegex())) {
            throw AppError.ValidationError(
                message = "Invalid phone number format. Please use international format (e.g., +1234567890).",
                field = "phoneNumber"
            )
        }
    }

    /**
     * Login user with phone number
     */
    override suspend fun login(
        identifier: String,
        password: String,
        isEmployer: Boolean,
        rememberMe: Boolean
    ): Flow<ApiResult<User>> = flow {
        emit(ApiResult.loading())

        try {
            // Validate phone number format
            validatePhoneNumber(identifier)

            // Determine user type
            val userType = if (isEmployer) UserType.EMPLOYER else UserType.EMPLOYEE

            // Check if user exists in database
            val userResponse = supabaseClient.client.postgrest.from("users")
                .select {
                    filter {
                        eq("phone_number", identifier)
                        eq("user_type", userType.name)
                    }
                }
                .decodeList<Map<String, Any>>()

            // Check if user exists
            if (userResponse.isEmpty()) {
                emit(ApiResult.Error(AppError.SecurityError(
                    message = "User not found. Please verify your phone number or register.",
                    securityDomain = "authentication"
                )))
                return@flow
            }

            // Extract user details
            val userDetails = userResponse.first()

            // Create User object
            val user = User(
                id = userDetails["id"] as String,
                name = userDetails["full_name"] as? String ?: "",
                email = userDetails["email"] as? String,
                phone = identifier,
                type = userType,
                profile = null
            )

            // If rememberMe is true, save authentication details
            if (rememberMe) {
                saveUserAuthenticationDetails(user)
            }

            // Log successful login
            logger.d(TAG, "User logged in successfully", mapOf(
                "user_id" to user.id,
                "user_type" to userType.name
            ))

            emit(ApiResult.Success(user))
        } catch (e: Exception) {
            logger.e(TAG, "Login failed", e)
            emit(ApiResult.Error(e.toAppError()))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Helper class to handle result from Firebase callbacks
     */
    private class ResultContainer<T> {
        var result: T? = null
        var exception: Exception? = null
    }

    /**
     * Extension function to convert Exception to AppError
     */
    private fun Exception.toAppError(): AppError {
        return when (this) {
            is AppError -> this
            is FirebaseException -> AppError.SecurityError(
                message = this.message ?: "Firebase authentication error",
                securityDomain = "authentication"
            )
            else -> AppError.UnexpectedError(
                message = this.message ?: "An unknown error occurred",
                cause = this,
                stackTrace = this.stackTraceToString()
            )
        }
    }
}
