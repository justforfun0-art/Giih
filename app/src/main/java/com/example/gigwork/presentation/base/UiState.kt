// presentation/base/UiState.kt
package com.example.gigwork.presentation.base

import com.example.gigwork.core.error.model.ErrorMessage

interface UiState<T : UiState<T>> {
    val isLoading: Boolean
    val errorMessage: ErrorMessage?

    fun copy(
        isLoading: Boolean = this.isLoading,
        errorMessage: ErrorMessage? = this.errorMessage
    ): T
}