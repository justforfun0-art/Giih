// presentation/navigation/Screen.kt
package com.example.gigwork.presentation.navigation

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Jobs : Screen("jobs")
    object JobDetails : Screen("jobs/{jobId}") {
        fun createRoute(jobId: String) = "jobs/$jobId"
    }
    object Profile : Screen("profile")
    object CreateJob : Screen("create_job")
    object EmployerJobs : Screen("employer_jobs")
    object EmployeeProfile : Screen("employee_profile")
    object EmployerProfile : Screen("employer_profile")
    object EmployerDashboard : Screen("employer_dashboard")
    object Settings : Screen("settings")
}
