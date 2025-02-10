// presentation/base/UiEvent.kt
package com.example.gigwork.presentation.base

interface UiEvent {
    fun getEventName(): String = this::class.simpleName ?: "UnknownEvent"

    fun getEventData(): Map<String, Any?> = emptyMap()

    fun isTracked(): Boolean = true
}

// Common events that can be reused
sealed class CommonUiEvent : UiEvent {
    data class ShowSnackbar(val message: String) : CommonUiEvent()
    data class ShowError(val message: String) : CommonUiEvent()
    object NavigateBack : CommonUiEvent()
    data class Refresh(val reason: String? = null) : CommonUiEvent()
    data class Loading(val isLoading: Boolean) : CommonUiEvent()
}