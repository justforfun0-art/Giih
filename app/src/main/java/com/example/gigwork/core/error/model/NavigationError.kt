// core/error/model/NavigationError.kt
package com.example.gigwork.core.error.model

sealed class NavigationError(
    override val message: String,
    override val cause: Throwable? = null,
    override val errorCode: String? = null
) : AppError(message, cause, errorCode) {

    data class UnauthorizedNavigation(
        val route: String,
        override val message: String = "Unauthorized access to route: $route",
        override val errorCode: String? = "NAV_UNAUTHORIZED"
    ) : NavigationError(message, null, errorCode)

    data class InvalidDeepLink(
        val uri: String,
        override val cause: Throwable? = null,
        override val message: String = "Invalid deep link: $uri",
        override val errorCode: String? = "NAV_INVALID_DEEPLINK"
    ) : NavigationError(message, cause, errorCode)

    data class NavigationFailed(
        val route: String,
        override val cause: Throwable,
        override val message: String = "Navigation failed to route: $route",
        override val errorCode: String? = "NAV_FAILED"
    ) : NavigationError(message, cause, errorCode)

    data class InvalidRoute(
        val route: String,
        override val message: String = "Invalid route: $route",
        override val errorCode: String? = "NAV_INVALID_ROUTE"
    ) : NavigationError(message, null, errorCode)
}