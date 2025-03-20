// presentation/navigation/NavigationExtensions.kt
package com.example.gigwork.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateAndPopUp(
    route: String,
    popUpTo: String,
    inclusive: Boolean = false
) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(popUpTo) {
            this.inclusive = inclusive
            saveState = true
        }
    }
}

fun NavController.navigateAndClearBackStack(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
            saveState = true
        }
    }
}

fun NavController.navigateWithArgs(
    baseRoute: String,
    vararg args: String
) {
    navigate(baseRoute.format(*args)) {
        launchSingleTop = true
        restoreState = true
    }
}