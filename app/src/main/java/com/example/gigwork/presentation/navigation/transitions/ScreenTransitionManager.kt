package com.example.gigwork.presentation.navigation.transitions

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.example.gigwork.presentation.navigation.Screen
import javax.inject.Inject

class ScreenTransitionManager @Inject constructor() {
    // Helper methods made public for use in AnimatedNavigation
    fun isModalScreen(route: String): Boolean {
        return route in setOf(
            Screen.CreateJob.route,
            Screen.Settings.route
        )
    }

    fun isDetailScreen(route: String): Boolean {
        return route.startsWith("jobs/") ||
                route.startsWith("employer/profile/") ||
                route.startsWith("employee/profile/")
    }

    // Original methods for ContentTransform
    fun getEnterTransition(route: String): ContentTransform {
        return when {
            isModalScreen(route) -> modalEnterTransition()
            isDetailScreen(route) -> detailEnterTransition()
            else -> defaultEnterTransition()
        }
    }

    fun getExitTransition(route: String): ContentTransform {
        return when {
            isModalScreen(route) -> modalExitTransition()
            isDetailScreen(route) -> detailExitTransition()
            else -> defaultExitTransition()
        }
    }

    // Methods for specific transition types
    fun getModalEnterTransition(): ContentTransform = modalEnterTransition()
    fun getModalExitTransition(): ContentTransform = modalExitTransition()
    fun getDetailEnterTransition(): ContentTransform = detailEnterTransition()
    fun getDetailExitTransition(): ContentTransform = detailExitTransition()
    fun getDefaultEnterTransition(): ContentTransform = defaultEnterTransition()
    fun getDefaultExitTransition(): ContentTransform = defaultExitTransition()

    private fun modalEnterTransition() = ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(300),
            initialOffsetX = { it }
        ),
        initialContentExit = fadeOut(animationSpec = tween(300))
    )

    private fun modalExitTransition() = ContentTransform(
        targetContentEnter = fadeIn(animationSpec = tween(300)),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(300),
            targetOffsetX = { -it }
        )
    )

    private fun detailEnterTransition() = ContentTransform(
        targetContentEnter = fadeIn(animationSpec = tween(300)),
        initialContentExit = fadeOut(animationSpec = tween(300))
    )

    private fun detailExitTransition() = ContentTransform(
        targetContentEnter = fadeIn(animationSpec = tween(300)),
        initialContentExit = fadeOut(animationSpec = tween(300))
    )

    private fun defaultEnterTransition() = ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(300),
            initialOffsetX = { it }
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(300),
            targetOffsetX = { -it }
        )
    )

    private fun defaultExitTransition() = ContentTransform(
        targetContentEnter = slideInHorizontally(
            animationSpec = tween(300),
            initialOffsetX = { -it }
        ),
        initialContentExit = slideOutHorizontally(
            animationSpec = tween(300),
            targetOffsetX = { it }
        )
    )
}