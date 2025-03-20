package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.viewModelScope
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.domain.models.UserRole.Companion.toUserRole
import com.example.gigwork.presentation.navigation.auth.AuthNavigationManager
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.launch

/**
 * Extension function to update PhoneAuthViewModel to integrate with AuthNavigationManager
 * This allows the auth flow to control navigation properly within the app architecture
 */
fun PhoneAuthViewModel.connectWithNavigation(authNavigationManager: AuthNavigationManager) {
    // Observe UI state changes for navigation
    viewModelScope.launch {
        // Use Flow collection in production code
        val state = this@connectWithNavigation.uiState.value

        if (state.isVerified) {
            // When verification is complete, navigate to profile completion
            authNavigationManager.navigateToProfileCompletion()
        }
    }

    // Optional: Add error handling integration if needed
    // Handle auth errors by delegating to authNavigationManager.handleAuthError
}

/**
 * Extension function to update AuthViewModel to integrate with AuthNavigationManager
 */
fun AuthViewModel.connectWithNavigation(authNavigationManager: AuthNavigationManager) {
    viewModelScope.launch {
        // Use Flow collection in production code
        val state = this@connectWithNavigation.uiState.value

        if (state.isLoggedIn && state.authState != null) {
            // Get user type from auth state
            val userType = state.authState?.userType?.let {
                UserType.valueOf(it)
            } ?: UserType.EMPLOYEE // Default to EMPLOYEE if null

            // Convert to UserRole and complete auth flow
            val userRole = userType.toUserRole()
            authNavigationManager.completeAuthFlow(userRole)
        }
    }

    // Handle logout with navigation
    fun logoutWithNavigation() {
        viewModelScope.launch {
            logout()
            authNavigationManager.startAuthFlow(clearBackStack = true)
        }
    }
}

/**
 * Extension function to update ProfileViewModel to integrate with AuthNavigationManager
 */
fun ProfileViewModel.connectWithNavigation(authNavigationManager: AuthNavigationManager) {
    viewModelScope.launch {
        // Use Flow collection in production code
        val state = this@connectWithNavigation.state.value

        if (state.isSuccess) {
            // Get user type from wherever it's stored in your ViewModel
            // For this example, we'll assume it's stored in some property
            val userType = UserType.EMPLOYEE // Replace with actual property

            // Convert to UserRole and complete auth flow
            val userRole = userType.toUserRole()
            authNavigationManager.completeAuthFlow(userRole)
        }
    }
}