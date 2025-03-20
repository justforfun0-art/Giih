package com.example.gigwork.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.presentation.navigation.AuthRoutes.OTP_VERIFICATION
import com.example.gigwork.presentation.navigation.AuthScreen.PhoneInput
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.screens.LoginScreen
import com.example.gigwork.presentation.screens.ProfileCompletionScreen
import com.example.gigwork.presentation.screens.auth.OtpVerificationScreen
import com.example.gigwork.presentation.screens.auth.PhoneNumberEntryScreen
import com.example.gigwork.presentation.screens.ProfileCompletionScreen

/**
 * Authentication Routes definition
 */
object AuthRoutes {
    // Base route for the auth navigation graph
    const val AUTH_GRAPH = "auth_graph"

    // Individual screen routes
    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFICATION = "otp_verification"
    const val PROFILE_COMPLETION = "profile_completion"
    const val LOGIN = "login"

    // Helper functions for constructing full routes
    fun getFullRoute(route: String) = "$AUTH_GRAPH/$route"

    fun phoneInput() = getFullRoute(PHONE_INPUT)
    fun otpVerification(phoneNumber: String? = null): String {
        return if (phoneNumber != null) {
            "$AUTH_GRAPH/$OTP_VERIFICATION?phoneNumber=$phoneNumber"
        } else {
            getFullRoute(OTP_VERIFICATION)
        }
    }
    fun profileCompletion() = getFullRoute(PROFILE_COMPLETION)
    fun login() = getFullRoute(LOGIN)
}

/**
 * Screen definitions for Auth flows
 */
sealed class AuthScreen(val route: String) {
    object PhoneInput : AuthScreen(AuthRoutes.phoneInput())
    object OtpVerification : AuthScreen(AuthRoutes.otpVerification())
    object ProfileCompletion : AuthScreen(AuthRoutes.profileCompletion())
    object Login : AuthScreen(AuthRoutes.login())

    // For compatibility with existing screen system
    fun toScreen(): Screen {
        return when (this) {
            is PhoneInput -> Screen.PhoneEntry
            is OtpVerification -> Screen.OtpVerification
            is ProfileCompletion -> Screen.ProfileSetup
            is Login -> Screen.Login
        }
    }
}

/**
 * Add Auth-related screen definitions to your Screen sealed class
 */
// Add these to your existing Screen sealed class:
/*
object PhoneInput : Screen(AuthRoutes.phoneInput())
object OtpVerification : Screen(AuthRoutes.otpVerification())
object ProfileCompletion : Screen(AuthRoutes.profileCompletion())
object Login : Screen(AuthRoutes.login())
*/

/**
 * Extension function to add the auth navigation graph to your NavGraphBuilder
 */
fun NavGraphBuilder.authNavGraph(
    navController: NavController,
    navigationCommands: NavigationCommands
) {
    navigation(
        startDestination = AuthRoutes.PHONE_INPUT,
        route = AuthRoutes.AUTH_GRAPH
    ) {
        // Phone Input Screen
        composable(route = AuthRoutes.PHONE_INPUT) {
            PhoneNumberEntryScreen(
                navController = navController
            )
        }

        // OTP Verification Screen
        composable(
            route = "$OTP_VERIFICATION?phoneNumber={phoneNumber}"
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber")

            phoneNumber?.let {
                OtpVerificationScreen(
                    navController = navController,
                    phoneNumber = it,
                    onVerificationComplete = { userId, userType ->
                        // Navigate to profile completion or other screens based on verification result
                        navController.navigate(AuthRoutes.PROFILE_COMPLETION) {
                            popUpTo(AuthRoutes.AUTH_GRAPH) { inclusive = true }
                        }
                    },
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // Profile Completion Screen
        composable(route = AuthRoutes.PROFILE_COMPLETION) {
            ProfileCompletionScreen(
                onProfileCompleted = { userRole ->
                    // Navigate to the appropriate starting screen based on user role
                    val destination = when (userRole) {
                        UserRole.EMPLOYER -> Screen.EmployerDashboard.route
                        UserRole.EMPLOYEE -> Screen.Jobs.route
                        UserRole.GUEST -> Screen.Welcome.route
                    }

                    // Navigate and clear back stack
                    navController.navigate(destination) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Login Screen (for returning users)
        // Replace this code in your authNavGraph function
        // Login Screen (for returning users)
        composable(route = AuthRoutes.LOGIN) {
            // Get the user type as a string
            val defaultUserType = UserType.EMPLOYEE.name // Convert the enum to a string

            LoginScreen(
                onNavigateBack = {
                    // Navigate to the phone input screen for signup
                    navController.navigate(AuthRoutes.PHONE_INPUT)
                },
                onNavigateToOtpVerification = { phoneNumber, verificationId ->
                    // Navigate to the OTP verification screen
                    navController.navigate(AuthRoutes.otpVerification(phoneNumber)) {
                        // Optional: Add navigation options here
                    }
                },
                userType = defaultUserType // Now passing a String, not an enum
            )
        }
    }
}