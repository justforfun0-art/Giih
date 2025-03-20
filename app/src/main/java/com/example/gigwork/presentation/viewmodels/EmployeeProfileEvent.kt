package com.example.gigwork.presentation.viewmodels

/**
 * Events emitted by the EmployeeProfileViewModel.
 */
sealed class EmployeeProfileEvent {
    /**
     * Navigate back to the previous screen
     */
    object NavigateBack : EmployeeProfileEvent()

    /**
     * Navigate to the settings screen
     */
    object NavigateToSettings : EmployeeProfileEvent()

    /**
     * User has logged out
     */
    object Logout : EmployeeProfileEvent()

    /**
     * Profile has been successfully updated
     */
    object ProfileUpdated : EmployeeProfileEvent()

    /**
     * Validation errors occurred during profile update
     */
    data class ValidationError(val errors: Map<String, String>) : EmployeeProfileEvent()
}