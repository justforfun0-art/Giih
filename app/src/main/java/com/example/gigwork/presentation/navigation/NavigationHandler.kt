package com.example.gigwork.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.gigwork.UserAction
import com.example.gigwork.presentation.navigation.state.NavigationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Handles navigation actions from UserAction objects.
 * This class wraps a NavigationState and provides methods to handle navigation-related UserActions.
 */
class NavigationHandler(
    private val navigationState: NavigationState,
    private val coroutineScope: CoroutineScope,
    private val navController: NavController
) {
    /**
     * Handles a UserAction, processing navigation actions and returning false
     * for actions that should be passed to the original handler.
     *
     * @return true if the action was handled, false otherwise
     */
    fun handleAction(action: UserAction): Boolean {
        return when (action) {
            is UserAction.Navigate -> {
                coroutineScope.launch {
                    navigationState.navigateTo(action.route)
                }
                true
            }
            is UserAction.NavigateBack -> {
                navController.navigateUp()
                true
            }
            is UserAction.ViewJob -> {
                val route = Screen.JobDetails.createRoute(action.jobId)
                coroutineScope.launch {
                    navigationState.navigateTo(route)
                }
                true
            }
            is UserAction.CreateJob -> {
                coroutineScope.launch {
                    navigationState.navigateTo(Screen.CreateJob.route)
                }
                true
            }
            is UserAction.ViewProfile -> {
                // You might need to adapt this based on user role
                coroutineScope.launch {
                    navigationState.navigateTo(Screen.Profile.route)
                }
                true
            }
            is UserAction.OpenSettings -> {
                coroutineScope.launch {
                    navigationState.navigateTo(Screen.Settings.route)
                }
                true
            }
            else -> false
        }
    }
}

/**
 * Creates and remembers a NavigationHandler.
 */
@Composable
fun rememberNavigationHandler(
    navigationState: NavigationState,
    navController: NavHostController
): NavigationHandler {
    val coroutineScope = rememberCoroutineScope()

    // Set up the NavController in the NavigationState
    LaunchedEffect(navController) {
        navigationState.setNavController(navController)
    }

    return remember(navigationState, coroutineScope, navController) {
        NavigationHandler(navigationState, coroutineScope, navController)
    }
}

/**
 * Creates a wrapped UserAction handler that processes navigation actions
 * and delegates other actions to the original handler.
 */
@Composable
fun rememberWrappedUserActionHandler(
    navigationHandler: NavigationHandler,
    originalHandler: (UserAction) -> Unit
): (UserAction) -> Unit {
    return remember(navigationHandler, originalHandler) {
        { action: UserAction ->
            if (!navigationHandler.handleAction(action)) {
                originalHandler(action)
            }
        }
    }
}