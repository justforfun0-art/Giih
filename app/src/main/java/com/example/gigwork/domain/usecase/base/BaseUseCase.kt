package com.example.gigwork.domain.usecase.base

import com.example.gigwork.core.error.model.AppError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import com.example.gigwork.di.IoDispatcher

/**
 * Result wrapper with Loading, Success and Error states
 */
sealed class Result<out R> {
    object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[error=$error]"
            Loading -> "Loading"
        }
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(error: AppError): Result<Nothing> = Error(error)
        fun loading(): Result<Nothing> = Loading
    }
}

/**
 * Interface for use cases that take parameters
 */
interface UseCase<in P, R> {
    suspend operator fun invoke(parameters: P): Flow<Result<R>>
}

/**
 * Interface for use cases that don't take parameters
 */
interface NoParamsUseCase<R> {
    suspend operator fun invoke(): Flow<Result<R>>
}

/**
 * Base class for Flow use cases with parameters
 */
abstract class FlowUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : UseCase<P, R> {

    final override suspend operator fun invoke(parameters: P): Flow<Result<R>> {
        return execute(parameters)
            .map { Result.success(it) }
            .onStart { emit(Result.loading()) }
            .catch { e -> emit(Result.error(e.toAppError())) }
            .flowOn(coroutineDispatcher)
    }

    protected abstract suspend fun execute(parameters: P): Flow<R>
}

/**
 * Base class for Flow use cases without parameters
 */
abstract class FlowNoParamsUseCase<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : NoParamsUseCase<R> {

    final override suspend operator fun invoke(): Flow<Result<R>> {
        return execute()
            .map { Result.success(it) }
            .onStart { emit(Result.loading()) }
            .catch { e -> emit(Result.error(e.toAppError())) }
            .flowOn(coroutineDispatcher)
    }

    protected abstract suspend fun execute(): Flow<R>
}

/**
 * Base class for single result use cases with parameters
 */
abstract class BaseUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : UseCase<P, R> {

    final override suspend operator fun invoke(parameters: P): Flow<Result<R>> = flow {
        emit(Result.loading())
        try {
            val result = execute(parameters)
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.error(e.toAppError()))
        }
    }.flowOn(coroutineDispatcher)

    protected abstract suspend fun execute(parameters: P): R
}

/**
 * Base class for single result use cases without parameters
 */
abstract class BaseNoParamsUseCase<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : NoParamsUseCase<R> {

    final override suspend operator fun invoke(): Flow<Result<R>> = flow {
        emit(Result.loading())
        try {
            val result = execute()
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.error(e.toAppError()))
        }
    }.flowOn(coroutineDispatcher)

    protected abstract suspend fun execute(): R
}

/**
 * Base class for synchronous use cases with parameters
 */
abstract class SyncUseCase<in P, R> {
    operator fun invoke(parameters: P): Result<R> = try {
        Result.success(execute(parameters))
    } catch (e: Exception) {
        Result.error(e.toAppError())
    }

    protected abstract fun execute(parameters: P): R
}

/**
 * Base class for synchronous use cases without parameters
 */
abstract class SyncNoParamsUseCase<R> {
    operator fun invoke(): Result<R> = try {
        Result.success(execute())
    } catch (e: Exception) {
        Result.error(e.toAppError())
    }

    protected abstract fun execute(): R
}

/**
 * Extensions for Result class
 */
suspend fun <T> Result<T>.onSuccess(
    action: suspend (T) -> Unit
): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

suspend fun <T> Result<T>.onError(
    action: suspend (AppError) -> Unit
): Result<T> {
    if (this is Result.Error) {
        action(error)
    }
    return this
}

suspend fun <T> Result<T>.onLoading(
    action: suspend () -> Unit
): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}

/**
 * Extension to convert throwable to AppError
 */
private fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    else -> AppError.UnexpectedError(
        message = message ?: "An unexpected error occurred",
        cause = this
    )
}
