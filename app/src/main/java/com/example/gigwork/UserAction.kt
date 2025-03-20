package com.example.gigwork

import com.example.gigwork.domain.models.UserProfile

/**
 * Represents actions that a user can take in the app.
 * Used to communicate from UI to ViewModel or between components.
 */
sealed class UserAction {
    // Existing actions
    data class Refresh(val userId: String) : UserAction()
    data class UpdateProfile(val profile: UserProfile) : UserAction()
    object Logout : UserAction()

    // New navigation actions
    data class Navigate(val route: String) : UserAction()
    object NavigateBack : UserAction()

    // Job actions
    data class ViewJob(val jobId: String) : UserAction()
    object CreateJob : UserAction()
    data class ApplyForJob(val jobId: String) : UserAction()

    // Profile actions
    object ViewProfile : UserAction()
    object EditProfile : UserAction()

    // Settings actions
    object OpenSettings : UserAction()
    data class UpdateSetting(val key: String, val value: Any) : UserAction()

    // Search actions
    data class Search(val query: String) : UserAction()
    data class ApplyFilter(val filters: Map<String, Any>) : UserAction()

    object Login : UserAction()
    data class VerifyOtp(val otp: String) : UserAction()
    data class SendOtp(val phoneNumber: String) : UserAction()


    // Error handling
    data class ReportError(val error: Throwable) : UserAction()

    // Add this to your UserAction.kt file
    data class NavigateToOtpVerification(val phoneNumber: String) : UserAction()

    // New actions for the improved flow
    object NavigateToEmployeeAuth : UserAction()
    object NavigateToEmployerAuth : UserAction()
}