package com.example.gigwork.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.gigwork.UserAction
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.components.GigWorkScaffold
import com.example.gigwork.presentation.navigation.AnimatedNavigation
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.navigation.transitions.ScreenTransitionManager
import com.example.gigwork.presentation.theme.GigWorkTheme
import coil.imageLoader

/**
 * Main entry point for the GigWork application UI.
 */
@Composable
fun GigWorkApp(
    userRole: UserRole,
    onUserAction: (UserAction) -> Unit,
    onError: (Throwable) -> Unit,
    startDestination: String = Screen.Welcome.route,
    navController: NavHostController = rememberNavController()
) {
    // Get the image loader from the context
    val context = LocalContext.current
    val imageLoader = remember { context.imageLoader }

    // Create navigation components
    val transitionManager = remember { ScreenTransitionManager() }

    GigWorkTheme {
        GigWorkScaffold(
            navController = navController,
            userRole = userRole,
            content = {
                AnimatedNavigation(
                    navController = navController,
                    startDestination = startDestination,
                    transitionManager = transitionManager,
                    imageLoader = imageLoader
                )
            }
        )
    }
}