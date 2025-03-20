package com.example.gigwork.domain.usecase.base

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import com.example.gigwork.di.IoDispatcher

interface UseCase<in P, R> {
    suspend operator fun invoke(parameters: P): Flow<ApiResult<R>>
}

interface NoParamsUseCase<R> {
    suspend operator fun invoke(): Flow<ApiResult<R>>
}

abstract class FlowUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : UseCase<P, R> {

    final override suspend operator fun invoke(parameters: P): Flow<ApiResult<R>> {
        return execute(parameters)
            .map { ApiResult.success(it) }
            .onStart { emit(ApiResult.loading()) }
            .catch { e -> emit(ApiResult.error(e.toAppError())) }
            .flowOn(coroutineDispatcher)
    }

    protected abstract suspend fun execute(parameters: P): Flow<R>
}

abstract class FlowNoParamsUseCase<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : NoParamsUseCase<R> {

    final override suspend operator fun invoke(): Flow<ApiResult<R>> {
        return execute()
            .map { ApiResult.success(it) }
            .onStart { emit(ApiResult.loading()) }
            .catch { e -> emit(ApiResult.error(e.toAppError())) }
            .flowOn(coroutineDispatcher)
    }

    protected abstract suspend fun execute(): Flow<R>
}

abstract class BaseUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : UseCase<P, R> {

    final override suspend operator fun invoke(parameters: P): Flow<ApiResult<R>> = flow {
        emit(ApiResult.loading())
        try {
            val result = execute(parameters)
            emit(ApiResult.success(result))
        } catch (e: Exception) {
            emit(ApiResult.error(e.toAppError()))
        }
    }.flowOn(coroutineDispatcher)

    protected abstract suspend fun execute(parameters: P): R
}

abstract class BaseNoParamsUseCase<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) : NoParamsUseCase<R> {

    final override suspend operator fun invoke(): Flow<ApiResult<R>> = flow {
        emit(ApiResult.loading())
        try {
            val result = execute()
            emit(ApiResult.success(result))
        } catch (e: Exception) {
            emit(ApiResult.error(e.toAppError()))
        }
    }.flowOn(coroutineDispatcher)

    protected abstract suspend fun execute(): R
}

abstract class SyncUseCase<in P, R> {
    operator fun invoke(parameters: P): ApiResult<R> = try {
        ApiResult.success(execute(parameters))
    } catch (e: Exception) {
        ApiResult.error(e.toAppError())
    }

    protected abstract fun execute(parameters: P): R
}

abstract class SyncNoParamsUseCase<R> {
    operator fun invoke(): ApiResult<R> = try {
        ApiResult.success(execute())
    } catch (e: Exception) {
        ApiResult.error(e.toAppError())
    }

    protected abstract fun execute(): R
}

private fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    else -> AppError.UnexpectedError(
        message = message ?: "An unexpected error occurred",
        cause = this
    )
}