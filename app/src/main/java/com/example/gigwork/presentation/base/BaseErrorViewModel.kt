package com.example.gigwork.presentation.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.ErrorAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.ErrorAction.Dismiss
import com.example.gigwork.core.error.model.ErrorAction.Multiple

/*data class ErrorMessage(
    val message: String,
    val title: String? = null,
    val level: ErrorLevel = ErrorLevel.ERROR,
    val action: ErrorAction? = null,
    val metadata: Map<String, String> = emptyMap()
)

enum class ErrorLevel {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

sealed class ErrorAction {
    object Retry : ErrorAction()
    object Dismiss : ErrorAction()
    data class Custom(val label: String, val route: String? = null) : ErrorAction()
    data class Multiple(
        val primary: ErrorAction,
        val secondary: ErrorAction? = null
    ) : ErrorAction()

    companion object {
        fun retryWithDismiss() = Multiple(Retry, Dismiss)
        fun login() = Custom(label = "Login", route = "login_screen")
    }
}*/

sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class Network(
        override val message: String = "Network error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Api(
        override val message: String = "API error occurred",
        val code: Int,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Validation(
        override val message: String = "Validation error occurred",
        val errors: Map<String, String> = emptyMap(),
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Database(
        override val message: String = "Database error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class UnexpectedError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}

interface ErrorHandler {
    fun handle(error: AppError): ErrorMessage
}

interface Logger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

abstract class BaseErrorViewModel<S : UiState<S>, E : UiEvent>(
    private val savedStateHandle: SavedStateHandle,
    private val errorHandler: ErrorHandler,
    private val logger: Logger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(createInitialState())
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = MutableSharedFlow<E>()
    val events: SharedFlow<E> = _events.asSharedFlow()

    abstract fun createInitialState(): S

    protected open fun setState(reducer: S.() -> S) {
        val newState = state.value.reducer()
        _state.value = newState
    }

    protected suspend fun emitEvent(event: E) {
        _events.emit(event)
    }

    protected fun safeLaunch(
        showLoading: Boolean = true,
        onError: (suspend (AppError) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                if (showLoading) updateLoading(true)
                withContext(dispatcher) { block() }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    val error = when (e) {
                        is AppError -> e
                        else -> AppError.UnexpectedError(message = e.message ?: "An unexpected error occurred", cause = e)
                    }
                    handleError(error)
                    onError?.invoke(error)
                }
            } finally {
                if (showLoading) updateLoading(false)
            }
        }
    }

    protected fun <T> safeFlow(
        showLoading: Boolean = true,
        onError: (suspend (AppError) -> Unit)? = null,
        block: Flow<T>
    ) {
        viewModelScope.launch {
            try {
                if (showLoading) updateLoading(true)
                block
                    .flowOn(dispatcher)
                    .catch { e ->
                        if (e !is CancellationException) {
                            val error = when (e) {
                                is AppError -> e
                                else -> AppError.UnexpectedError(message = e.message ?: "An unexpected error occurred", cause = e)
                            }
                            handleError(error)
                            onError?.invoke(error)
                        }
                    }
                    .collect()
            } finally {
                if (showLoading) updateLoading(false)
            }
        }
    }

    protected suspend fun <T> safeCall(
        showLoading: Boolean = true,
        onError: (suspend (AppError) -> Unit)? = null,
        block: suspend () -> T
    ): T? {
        return try {
            if (showLoading) updateLoading(true)
            withContext(dispatcher) { block() }
        } catch (e: Exception) {
            if (e !is CancellationException) {
                val error = when (e) {
                    is AppError -> e
                    else -> AppError.UnexpectedError(message = e.message ?: "An unexpected error occurred", cause = e)
                }
                handleError(error)
                onError?.invoke(error)
            }
            null
        } finally {
            if (showLoading) updateLoading(false)
        }
    }

    private fun updateLoading(loading: Boolean) {
        setState {
            @Suppress("UNCHECKED_CAST")
            this.copy(isLoading = loading) as S
        }
    }

    // Update the handleError function
    protected fun handleError(error: AppError, key: String = "default") {
        val errorMessage = errorHandler.handle(error)
        setState {
            copy(errorMessage = errorMessage)
        }
        logError(error)
    }
    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is Dismiss -> clearError()
            is Multiple -> handleErrorAction(action.primary)
            else -> clearError()
        }
    }

     fun clearError() {
        setState {
            @Suppress("UNCHECKED_CAST")
            this.copy(errorMessage = null) as S
        }
    }

    protected fun <T> getSavedState(key: String, default: T): T {
        return savedStateHandle.get<T>(key) ?: default
    }

    protected fun <T> setSavedState(key: String, value: T) {
        savedStateHandle[key] = value
    }

    protected fun clearSavedState(key: String) {
        savedStateHandle.remove<Any>(key)
    }

    protected fun logDebug(tag: String, message: String) {
        logger.debug(tag, message)
    }

    protected fun logError(error: AppError) {
        logger.error(
            tag = error::class.simpleName ?: "Error",
            message = error.message,
            throwable = error.cause
        )
    }

    protected fun logError(tag: String, message: String, throwable: Throwable? = null) {
        logger.error(tag, message, throwable)
    }

    override fun onCleared() {
        super.onCleared()
        clearAllSavedState()
    }

    protected open fun clearAllSavedState() {
        // Override to clear specific saved state
    }
}