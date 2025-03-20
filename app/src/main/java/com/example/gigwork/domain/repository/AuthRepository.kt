package com.example.gigwork.domain.repository

import android.app.Activity
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.AuthState
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserType
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /**
     * Send OTP verification code to phone number
     * @param phoneNumber Phone number to send OTP
     * @param activity Android Activity required for reCAPTCHA verification
     * @return Flow of ApiResult with verification ID
     */
    fun sendOtpToPhone(
        phoneNumber: String,
        activity: Activity?
    ): Flow<ApiResult<String>>

    /**
     * Verify OTP code
     * @param verificationId Verification ID from sendOtpToPhone
     * @param otp One-time password to verify
     * @return Flow of ApiResult with AuthState
     */
    fun verifyOtp(
        verificationId: String,
        otp: String
    ): Flow<ApiResult<AuthState>>

    /**
     * Login user
     * @param identifier Phone number or email
     * @param password Unused in OTP-based authentication
     * @param isEmployer Determines user type
     * @param rememberMe Save authentication details
     * @return Flow of ApiResult with User
     */
    suspend fun login(
        identifier: String,
        password: String,
        isEmployer: Boolean,
        rememberMe: Boolean
    ): Flow<ApiResult<User>>

    /**
     * Save user type (employee or employer)
     * @param userType User type to save
     * @return Flow of ApiResult indicating success or failure
     */
    fun saveUserType(userType: UserType): Flow<ApiResult<Unit>>

    /**
     * Get current authenticated user information
     * @return Flow of ApiResult with current AuthState
     */
    fun getCurrentUser(): Flow<ApiResult<AuthState>>

    /**
     * Logout the current user
     * @return Flow of ApiResult indicating logout success or failure
     */
    fun logout(): Flow<ApiResult<Unit>>

    /**
     * Clear stored authentication data
     */
    fun clearAuthData()

    /**
     * Check if user is currently logged in
     * @return Boolean indicating login status
     */
    fun isUserLoggedIn(): Boolean

    /**
     * Create or link Supabase account
     * @param firebaseUser Firebase user to create/link
     * @param userType User type
     * @return ApiResult indicating success or failure
     */
    suspend fun createOrLinkSupabaseAccount(
        firebaseUser: FirebaseUser,
        userType: UserType
    ): ApiResult<Unit>
}