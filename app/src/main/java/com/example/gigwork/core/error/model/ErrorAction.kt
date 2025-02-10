package com.example.gigwork.core.error.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class ErrorAction : Parcelable {
    @Parcelize
    object Retry : ErrorAction()

    @Parcelize
    object Dismiss : ErrorAction()

    @Parcelize
    object GoBack : ErrorAction()

    @Parcelize
    data class Custom(
        val label: String,
        val route: String? = null,
        val data: Map<String, String> = emptyMap()
    ) : ErrorAction()

    @Parcelize
    data class Multiple(
        val primary: ErrorAction,
        val secondary: ErrorAction
    ) : ErrorAction()

    companion object {
        fun login(returnRoute: String? = null) = Custom(
            label = "Login",
            route = "login_screen",
            data = mapOf("return_route" to (returnRoute ?: ""))
        )

        fun retry(label: String = "Retry") = Custom(label = label)

        fun settings(section: String? = null) = Custom(
            label = "Open Settings",
            route = "settings_screen",
            data = section?.let { mapOf("section" to it) } ?: emptyMap()
        )

        fun contact(type: String) = Custom(
            label = "Contact Support",
            route = "support_screen",
            data = mapOf("type" to type)
        )

        fun retryWithDismiss() = Multiple(
            primary = Retry,
            secondary = Dismiss
        )

        fun customWithDismiss(
            label: String,
            route: String? = null,
            data: Map<String, String> = emptyMap()
        ) = Multiple(
            primary = Custom(label, route, data),
            secondary = Dismiss
        )
    }

    fun requiresNavigation(): Boolean {
        return when(this) {
            is Retry -> false
            is Dismiss -> false
            is GoBack -> true
            is Custom -> route != null
            is Multiple -> primary.requiresNavigation() || secondary.requiresNavigation()
        }
    }

    fun isDestructive(): Boolean {
        return when(this) {
            is Retry -> false
            is Dismiss -> false
            is GoBack -> false
            is Custom -> data["destructive"]?.toBoolean() ?: false
            is Multiple -> primary.isDestructive() || secondary.isDestructive()
        }
    }

    fun getAnalyticsLabel(): String {
        return when(this) {
            is Retry -> "retry"
            is Dismiss -> "dismiss"
            is GoBack -> "go_back"
            is Custom -> "custom_${label.lowercase().replace(" ", "_")}"
            is Multiple -> "multiple_${primary.getAnalyticsLabel()}_${secondary.getAnalyticsLabel()}"
        }
    }

    fun getAccessibilityDescription(): String {
        return when(this) {
            is Retry -> "Retry the operation"
            is Dismiss -> "Dismiss the error"
            is GoBack -> "Go back to previous screen"
            is Custom -> label
            is Multiple -> primary.getAccessibilityDescription()
        }
    }
}