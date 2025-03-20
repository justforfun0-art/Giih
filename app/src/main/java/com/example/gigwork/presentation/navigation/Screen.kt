// presentation/navigation/Screen.kt
package com.example.gigwork.presentation.navigation

import com.example.gigwork.domain.models.UserType
import com.example.gigwork.util.Constants

sealed class Screen(val route: String) {

    object Welcome : Screen("welcome")

    object Login : Screen("login/{userType}") {
        fun createRoute(userType: String) = "login/$userType"
        fun createRouteWithUserType(userType: String) = "login/$userType"
    }

    object Signup : Screen("signup/{userType}") {
        fun createRoute(userType: String) = "signup/$userType"
        fun createRouteWithUserType(userType: String) = "signup/$userType"
    }

    object Jobs : Screen("jobs") { // Remove the query parameter
        fun createRoute() = "jobs"
    }

    object JobDetails : Screen("jobs/{jobId}") {
        fun createRoute(jobId: String) = "jobs/$jobId"
    }

    object Profile : Screen("profile?userType={userType}") {
        // Create helper method to build route with parameters
        fun createRoute(userType: String): String {
            return "profile?userType=$userType"
        }
    }

    object CreateJob : Screen("create_job")

    object EmployerJobs : Screen("employer_jobs")

    object EmployeeProfile : Screen("employee/profile/{employeeId}") {
        fun createRoute(employeeId: String) = "employee/profile/$employeeId"
    }

    object EmployerProfile : Screen("employer/profile/{employerId}") {
        fun createRoute(employerId: String) = "employer/profile/$employerId"
    }

    object EmployerDashboard : Screen("employer_dashboard")

    object Settings : Screen("settings")

    object PhoneEntry : Screen("phone_entry?userType={userType}") {
        fun createRouteWithUserType(userType: String) = "phone_entry?userType=$userType"
    }

    object OtpVerification : Screen("otp_verification/{phoneNumber}/{verificationId}/{userType}") {
        fun createRoute(phoneNumber: String, verificationId: String, userType: String) =
            "otp_verification/$phoneNumber/$verificationId/$userType"
    }

    object ProfileSetup : Screen("profile_setup/{userId}/{userType}") {
        fun createRoute(userId: String, userType: String) = "profile_setup/$userId/$userType"
    }

    object CreateProfile : Screen("profile/create/{type}/{userId}") {
        fun createRoute(userId: String, userType: String) = "profile/create/$userType/$userId"
        fun createEmployerRoute(userId: String) = "profile/create/${Constants.UserType.EMPLOYER}/$userId"
        fun createEmployeeRoute(userId: String) = "profile/create/${Constants.UserType.EMPLOYEE}/$userId"
    }
}