package com.example.gigwork.presentation.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Base interface for UI State with copy support
 * @param T the concrete type implementing this interface (must be a data class)
 */

/**
 * Base interface for UI Events
 */
/**
 * Error message for UI display
 */
data class ErrorMessage(
    val message: String,
    val title: String? = null,
    val level: ErrorLevel = ErrorLevel.ERROR,
    val action: ErrorAction? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Error levels indicating severity
 */
enum class ErrorLevel {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Actions that can be taken in response to errors
 */
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
}

/**
 * Represents different types of app errors
 */
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

/**
 * Interface for global error handling
 */
interface ErrorHandler {
    fun handle(error: AppError): ErrorMessage
}

/**
 * Interface for logging
 */
interface Logger {
    fun debug(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Base ViewModel providing common functionality for state management, error handling,
 * and coroutine management
 */
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

    /**
     * Create initial UI state - must be implemented by subclasses
     */
    abstract fun createInitialState(): S

    /**
     * Update state using a reducer function
     */
    protected fun setState(reducer: S.() -> S) {
        val newState = state.value.reducer()
        _state.value = newState
    }

    /**
     * Emit a one-time event
     */
    protected suspend fun emitEvent(event: E) {
        _events.emit(event)
    }

    /**
     * Launch a coroutine with error handling and loading state
     */
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

    /**
     * Execute a flow with error handling and loading state
     */
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

    /**
     * Execute a suspending operation that returns a result with error handling
     */
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

    protected fun handleError(error: AppError) {
        val errorMessage = errorHandler.handle(error)
        setState {
            @Suppress("UNCHECKED_CAST")
            this.copy(errorMessage = ErrorMessage) as S
        }
        logError(error)
    }

    protected fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Dismiss -> clearError()
            is ErrorAction.Multiple -> handleErrorAction(action.primary)
            else -> clearError()
        }
    }

    protected fun clearError() {
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

    /**
     * Clear all saved state - override in subclasses if needed
     */
    protected open fun clearAllSavedState() {
        // Override to clear specific saved state
    }
}