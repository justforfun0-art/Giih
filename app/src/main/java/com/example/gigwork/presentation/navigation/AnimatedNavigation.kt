package com.example.gigwork.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import coil.ImageLoader
import com.example.gigwork.presentation.navigation.transitions.ScreenTransitionManager

object NavigationBuilders {
    private val appNavigation = AppNavigationBuilders()

    // In NavigationBuilders.kt
    fun NavGraphBuilder.addWelcomeScreen(navController: NavHostController) {
        appNavigation.addWelcomeScreen(this, navController)
    }

    fun NavGraphBuilder.addAuthFlow(navController: NavHostController) {
        appNavigation.addAuthFlow(this, navController)
    }

    fun NavGraphBuilder.addProfileFlow(navController: NavHostController) {
        appNavigation.addProfileFlow(this, navController)
    }

    fun NavGraphBuilder.addMainFlow(
        navController: NavHostController,
        imageLoader: ImageLoader
    ) {
        appNavigation.addMainFlow(this, navController, imageLoader)
    }

    fun NavGraphBuilder.addCommonScreens(navController: NavHostController) {
        appNavigation.addCommonScreens(this, navController)
    }


}

@Composable
fun AnimatedNavigation(
    navController: NavHostController,
    startDestination: String,
    transitionManager: ScreenTransitionManager,
    imageLoader: ImageLoader
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            when {
                transitionManager.isModalScreen(targetState.destination.route ?: "") ->
                    fadeIn(tween(300))
                else -> fadeIn(tween(300)) + slideInHorizontally(
                    tween(300),
                    initialOffsetX = { 1000 }
                )
            }
        },
        exitTransition = {
            when {
                transitionManager.isModalScreen(initialState.destination.route ?: "") ->
                    fadeOut(tween(300))
                else -> fadeOut(tween(300)) + slideOutHorizontally(
                    tween(300),
                    targetOffsetX = { -1000 }
                )
            }
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideInHorizontally(
                tween(300),
                initialOffsetX = { -1000 }
            )
        },
        popExitTransition = {
            fadeOut(tween(300)) + slideOutHorizontally(
                tween(300),
                targetOffsetX = { 1000 }
            )
        }
    ) {
        with(NavigationBuilders) {
            addWelcomeScreen(navController)
            addAuthFlow(navController)
            addProfileFlow(navController)
            addMainFlow(navController, imageLoader)
            addCommonScreens(navController)
        }
    }
}