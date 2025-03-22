package com.example.gigwork.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.gigwork.presentation.screens.*
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.util.Constants
import coil.ImageLoader
import com.example.gigwork.presentation.screens.auth.EmployeeProfileScreen
import com.example.gigwork.presentation.screens.auth.EmployerDashboardScreen
import com.example.gigwork.presentation.screens.auth.EmployerProfileScreen
import com.example.gigwork.presentation.screens.auth.OtpVerificationScreen
import com.example.gigwork.presentation.screens.SignupScreen
import com.example.gigwork.presentation.screens.auth.WelcomeScreen

import com.example.gigwork.presentation.viewmodels.AuthViewModel

class AppNavigationBuilders {


    // In AppNavigation.kt - addWelcomeScreen()
    fun addWelcomeScreen(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        navGraphBuilder.composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToEmployeeAuth = {
                    navController.navigate(Screen.Login.createRoute(UserType.EMPLOYEE.name)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true } // Clear back stack
                        launchSingleTop = true
                    }
                },
                onNavigateToEmployerAuth = {
                    navController.navigate(Screen.Login.createRoute(UserType.EMPLOYER.name)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }

    fun addAuthFlow(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        // Login screen with mobile OTP verification
        navGraphBuilder.composable(
            route = Screen.Login.route,
            arguments = listOf(navArgument("userType") {
                type = NavType.StringType
                defaultValue = UserType.EMPLOYEE.name
            })
        ) { backStackEntry ->
            val userTypeStr =
                backStackEntry.arguments?.getString("userType") ?: UserType.EMPLOYEE.name

            LoginScreen(
                userType = userTypeStr,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToOtpVerification = { phoneNumber, verificationId ->
                    navController.navigate(
                        Screen.OtpVerification.createRoute(phoneNumber, verificationId, userTypeStr)
                    )
                }
            )
        }

        // Signup screen
        navGraphBuilder.composable(
            route = Screen.Signup.route,
            arguments = listOf(navArgument("userType") {
                type = NavType.StringType
                defaultValue = UserType.EMPLOYEE.name
            })
        ) { backStackEntry ->
            val userTypeStr =
                backStackEntry.arguments?.getString("userType") ?: UserType.EMPLOYEE.name

            SignupScreen(
                userType = userTypeStr, // Use the string version instead of converting to enum
                onNavigateBack = { navController.navigateUp() },
                onNavigateToOtpVerification = { phoneNumber, verificationId ->
                    navController.navigate(
                        Screen.OtpVerification.createRoute(phoneNumber, verificationId, userTypeStr)
                    )
                },
                onNavigateToLogin = { // Add this missing parameter
                    navController.navigate(Screen.Login.createRoute(userTypeStr))
                }
            )
        }

        // OTP verification screen
        navGraphBuilder.composable(
            route = Screen.OtpVerification.route,
            arguments = listOf(
                navArgument("phoneNumber") { type = NavType.StringType },
                navArgument("verificationId") { type = NavType.StringType },
                navArgument("userType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val userType = backStackEntry.arguments?.getString("userType") ?: UserType.EMPLOYEE.name

            OtpVerificationScreen(
                navController = navController,
                phoneNumber = phoneNumber,
                verificationId = verificationId,
                userType = userType,
                onVerificationComplete = { userId, userTypeVal ->
                    navController.navigate(Screen.ProfileSetup.createRoute(userId, userTypeVal)) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBackPressed = { navController.navigateUp() }
            )
        }

        // Profile setup screen
        navGraphBuilder.composable(
            route = Screen.ProfileSetup.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("userType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userType = backStackEntry.arguments?.getString("userType") ?: UserType.EMPLOYEE.name

            ProfileScreen(
                userId = userId,
                userType = userType,
                onNavigateBack = {
                    val destination = if (userType == UserType.EMPLOYER.name) {
                        Screen.EmployerDashboard.route
                    } else {
                        Screen.Jobs.route
                    }
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
    fun addProfileFlow(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        // Profile screen for viewing/editing existing profile
        navGraphBuilder.composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userType") {
                type = NavType.StringType
                defaultValue = UserType.EMPLOYEE.name
            })
        ) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: UserType.EMPLOYEE.name

            ProfileScreen(
                userId = "", // You would need to pass this from user session
                userType = userType,
                onNavigateBack = {
                    // Navigate based on user type
                    if (userType == UserType.EMPLOYER.name) {
                        navController.navigate(Screen.EmployerDashboard.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Jobs.route) {
                            popUpTo(Screen.Profile.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Employer profile screen - for viewing other employers' profiles
        navGraphBuilder.composable(
            route = Screen.EmployerProfile.route,
            arguments = listOf(navArgument("employerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("employerId") ?: return@composable
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

        // Employee profile screen - for viewing other employees' profiles
        navGraphBuilder.composable(
            route = Screen.EmployeeProfile.route,
            arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("employeeId") ?: return@composable
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

        fun addMainFlow(
            navGraphBuilder: NavGraphBuilder,
            navController: NavHostController,
            imageLoader: ImageLoader?
        ) {
            // Jobs listing screen
            // In addMainFlow method
            // In addMainFlow method
            navGraphBuilder.composable(Screen.Jobs.route) {
                val authViewModel = hiltViewModel<AuthViewModel>()
                val userId = authViewModel.uiState.collectAsState().value.authState?.userId ?: ""

                val context = LocalContext.current
                JobsScreen(
                    userId = userId,
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetails.createRoute(jobId))
                    },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    },
                    imageLoader = imageLoader ?: ImageLoader.Builder(context).build()
                )
            }

            // Job details screen with deep linking
            navGraphBuilder.composable(
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

            // Employer dashboard
            navGraphBuilder.composable(Screen.EmployerDashboard.route) {
                EmployerDashboardScreen(
                    onNavigateToCreateJob = { navController.navigate(Screen.CreateJob.route) },
                    onNavigateToJobs = { navController.navigate(Screen.EmployerJobs.route) },
                    onNavigateToProfile = { userId ->
                        navController.navigate(Screen.EmployerProfile.createRoute(userId))
                    }
                )
            }

            // Employer's jobs listing
            navGraphBuilder.composable(Screen.EmployerJobs.route) {
                EmployerJobsScreen(
                    onNavigateToCreateJob = { navController.navigate(Screen.CreateJob.route) },
                    onJobSelected = { jobId ->
                        navController.navigate(Screen.JobDetails.createRoute(jobId))
                    },
                    onNavigateBack = { navController.navigateUp() }
                )
            }

            // Create job screen
            navGraphBuilder.composable(Screen.CreateJob.route) {
                CreateJobScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
        }

        // Inside AppNavigationBuilders class
        fun addCommonScreens(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
            navGraphBuilder.composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }