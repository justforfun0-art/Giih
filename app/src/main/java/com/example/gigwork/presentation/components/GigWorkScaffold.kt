package com.example.gigwork.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GigWorkScaffold(
    navController: NavHostController,
    userRole: UserRole,
    content: @Composable () -> Unit
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        topBar = {
            if (shouldShowTopBar(currentRoute)) {
                GigWorkTopAppBar(
                    title = getTitleForRoute(currentRoute ?: ""),
                    onNavigateBack = if (navController.previousBackStackEntry != null) {
                        { navController.navigateUp() }
                    } else null,
                    onProfileClick = {
                        val route = when (userRole) {
                            UserRole.EMPLOYER -> Screen.EmployerProfile.route
                            UserRole.EMPLOYEE -> Screen.EmployeeProfile.route
                            UserRole.GUEST -> Screen.Login.route
                        }
                        navController.navigate(route)
                    },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onLogout = {
                        // Clear backstack and navigate to Welcome screen
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomBar(currentRoute, userRole)) {
                GigWorkBottomNavigation(
                    currentRoute = currentRoute ?: "",
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (shouldShowFab(currentRoute, userRole)) {
                GigWorkFloatingActionButton(
                    onCreateJob = { navController.navigate(Screen.CreateJob.route) },
                    onCreateDraft = { navController.navigate(Screen.CreateJob.route + "?isDraft=true") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

private fun shouldShowTopBar(route: String?): Boolean {
    return when (route) {
        Screen.Welcome.route,
        Screen.Login.route,
        Screen.Signup.route -> false
        else -> true
    }
}

private fun shouldShowBottomBar(route: String?, userRole: UserRole): Boolean {
    if (route == null) return false
    return when {
        route in listOf(
            Screen.Welcome.route,
            Screen.Login.route,
            Screen.Signup.route
        ) -> false
        userRole == UserRole.GUEST -> false
        else -> true
    }
}

private fun shouldShowFab(route: String?, userRole: UserRole): Boolean {
    if (route == null) return false
    return userRole == UserRole.EMPLOYER && route == Screen.EmployerJobs.route
}

private fun getTitleForRoute(route: String): String {
    return when {
        route.startsWith(Screen.JobDetails.route) -> "Job Details"
        route.startsWith(Screen.EmployerProfile.route) -> "Employer Profile"
        route.startsWith(Screen.EmployeeProfile.route) -> "Employee Profile"
        route == Screen.Jobs.route -> "Available Jobs"
        route == Screen.CreateJob.route -> "Create Job"
        route == Screen.EmployerJobs.route -> "My Jobs"
        route == Screen.EmployerDashboard.route -> "Dashboard"
        route == Screen.Settings.route -> "Settings"
        else -> ""
    }
}