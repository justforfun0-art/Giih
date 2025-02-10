package com.example.gigwork.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.gigwork.presentation.screens.*

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        addAuthFlow(navController)
        addProfileFlow(navController)
        addMainFlow(navController)
        addCommonScreens(navController)
    }
}

private fun NavGraphBuilder.addAuthFlow(navController: NavHostController) {
    composable(Screen.Welcome.route) {
        WelcomeScreen(
            onNavigateToLogin = { navController.navigate(Screen.Login.route) },
            onNavigateToSignup = { navController.navigate(Screen.Signup.route) }
        )
    }

    composable(Screen.Login.route) {
        LoginScreen(
            onLoginSuccess = { isEmployer ->
                val destination = if (isEmployer) {
                    Screen.EmployerDashboard.route
                } else {
                    Screen.Jobs.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(Screen.Signup.route) {
        SignupScreen(
            onSignupSuccess = { isEmployer, userId ->
                val destination = if (isEmployer) {
                    Screen.CreateProfile.createEmployerRoute(userId)
                } else {
                    Screen.CreateProfile.createEmployeeRoute(userId)
                }
                navController.navigate(destination) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }
}

private fun NavGraphBuilder.addProfileFlow(navController: NavHostController) {
    composable(
        route = Screen.CreateProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("type") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val isEmployer = backStackEntry.arguments?.getString("type") == "employer"

        CreateProfileScreen(
            userId = userId,
            isEmployer = isEmployer,
            onProfileCreated = {
                val destination = if (isEmployer) {
                    Screen.EmployerDashboard.route
                } else {
                    Screen.Jobs.route
                }
                navController.navigate(destination) {
                    popUpTo(Screen.CreateProfile.route) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = Screen.EmployerProfile.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        EmployerProfileScreen(
            userId = userId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            onLogout = {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    composable(
        route = Screen.EmployeeProfile.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        EmployeeProfileScreen(
            userId = userId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            onLogout = {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

private fun NavGraphBuilder.addMainFlow(navController: NavHostController) {
    composable(Screen.Jobs.route) {
        JobsScreen(
            onJobSelected = { jobId ->
                navController.navigate(Screen.JobDetails.createRoute(jobId))
            },
            onNavigateToProfile = { userId ->
                navController.navigate(Screen.EmployeeProfile.createRoute(userId))
            }
        )
    }

    composable(
        route = Screen.JobDetails.route,
        arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
        deepLinks = listOf(
            navDeepLink {
                uriPattern = "gigwork://jobs/{jobId}"
                action = Intent.ACTION_VIEW
            }
        )
    ) { backStackEntry ->
        val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
        JobDetailsScreen(
            jobId = jobId,
            onNavigateBack = { navController.navigateUp() },
            onNavigateToEmployer = { employerId ->
                navController.navigate(Screen.EmployerProfile.createRoute(employerId))
            }
        )
    }

    composable(Screen.EmployerDashboard.route) {
        EmployerDashboardScreen(
            onNavigateToCreateJob = { navController.navigate(Screen.CreateJob.route) },
            onNavigateToJobs = { navController.navigate(Screen.EmployerJobs.route) },
            onNavigateToProfile = { userId ->
                navController.navigate(Screen.EmployerProfile.createRoute(userId))
            }
        )
    }

    composable(Screen.EmployerJobs.route) {
        EmployerJobsScreen(
            onNavigateToCreateJob = { navController.navigate(Screen.CreateJob.route) },
            onJobSelected = { jobId ->
                navController.navigate(Screen.JobDetails.createRoute(jobId))
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }

    composable(Screen.CreateJob.route) {
        CreateJobScreen(
            onJobCreated = {
                navController.navigate(Screen.EmployerJobs.route) {
                    popUpTo(Screen.CreateJob.route) { inclusive = true }
                }
            },
            onNavigateBack = { navController.navigateUp() }
        )
    }
}

private fun NavGraphBuilder.addCommonScreens(navController: NavHostController) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            onNavigateBack = { navController.navigateUp() }
        )
    }
}

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Jobs : Screen("jobs")

    object JobDetails : Screen("jobs/{jobId}") {
        fun createRoute(jobId: String) = "jobs/$jobId"
    }

    object CreateProfile : Screen("profile/create/{type}/{userId}") {
        fun createEmployerRoute(userId: String) = "profile/create/employer/$userId"
        fun createEmployeeRoute(userId: String) = "profile/create/employee/$userId"
    }

    object CreateJob : Screen("jobs/create")
    object EmployerJobs : Screen("employer/jobs")

    object EmployerProfile : Screen("employer/profile/{userId}") {
        fun createRoute(userId: String) = "employer/profile/$userId"
    }

    object EmployeeProfile : Screen("employee/profile/{userId}") {
        fun createRoute(userId: String) = "employee/profile/$userId"
    }

    object EmployerDashboard : Screen("employer/dashboard")
    object Settings : Screen("settings")
}