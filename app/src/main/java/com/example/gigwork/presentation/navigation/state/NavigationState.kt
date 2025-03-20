package com.example.gigwork.presentation.navigation.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Stable
@Singleton
class NavigationState @Inject constructor(
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @NavigationScope private val navigationScope: CoroutineScope
) : NavigationCommands {
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute.asStateFlow()

    private var _isNavigating by mutableStateOf(false)
    val isNavigating: Boolean get() = _isNavigating

    // NavController set by NavigationStateEffect
    private var navController: NavController? = null

    /**
     * Sets the NavController to be used for navigation.
     * This method should be called from a Composable using LaunchedEffect.
     */
    fun setNavController(controller: NavController) {
        navController = controller
        _currentRoute.value = controller.currentDestination?.route

        logger.d(
            tag = TAG,
            message = "NavController set with current route: ${_currentRoute.value}",
            additionalData = emptyMap()
        )
    }

    override suspend fun navigateTo(route: String, options: NavOptionsBuilder.() -> Unit) {
        try {
            _isNavigating = true

            // Actually perform the navigation using the NavController
            navController?.navigate(route, options) ?: run {
                logger.w(
                    tag = TAG,
                    message = "Navigation attempted but NavController is null",
                    additionalData = mapOf("route" to route)
                )
            }

            _currentRoute.value = route

            logger.d(
                tag = TAG,
                message = "Navigating to route: $route",
                additionalData = mapOf(
                    "previous_route" to _currentRoute.value,
                    "has_options" to (options != {})
                )
            )
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Navigation failed",
                throwable = e,
                additionalData = mapOf("route" to route)
            )
            throw e
        } finally {
            _isNavigating = false
        }
    }

    /**
     * Non-suspending version of navigateTo for use from non-coroutine contexts.
     */
    fun navigateTo(route: String) {
        navigationScope.launch {
            navigateTo(route) {}
        }
    }

    companion object {
        private const val TAG = "NavigationState"
    }
}