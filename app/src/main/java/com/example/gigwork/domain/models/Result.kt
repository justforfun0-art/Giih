// core/result/Result.kt
package com.example.gigwork.core.result

import com.example.gigwork.core.error.model.AppError

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: AppError) : Result<Nothing>()
    object Loading : Result<Nothing>()

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(error: AppError): Result<Nothing> = Error(error)
        fun loading(): Result<Nothing> = Loading
    }

    // Utility functions
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    inline fun onError(action: (AppError) -> Unit): Result<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun errorOrNull(): AppError? = when (this) {
        is Error -> error
        else -> null
    }

    fun isSuccess() = this is Success
    fun isError() = this is Error
    fun isLoading() = this is Loading
}