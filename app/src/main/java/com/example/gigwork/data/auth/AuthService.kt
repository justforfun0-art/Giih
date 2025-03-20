// path: com/example/gigwork/data/auth/AuthService.kt
package com.example.gigwork.data.auth

import android.app.Activity
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.AuthState
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow

interface AuthService {
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getAuthData(): AuthState
    suspend fun clearAuthData()
    suspend fun saveAuthData(token: String, userId: String, userType: String)

    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    )

    fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): Flow<ApiResult<String>>

    suspend fun getUserType(userId: String): Flow<ApiResult<String>>

    suspend fun updateUserType(userId: String, userType: String): Flow<ApiResult<Unit>>

    fun getUserPreferences(): Map<String, String>

    suspend fun register(
        phone: String,
        email: String? = null,
        password: String
    ): String
}