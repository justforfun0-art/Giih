package com.example.gigwork.core.result

import com.example.gigwork.core.error.model.AppError

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val error: AppError) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()

    companion object {
        fun <T> success(data: T): ApiResult<T> = Success(data)
        fun error(error: AppError): ApiResult<Nothing> = Error(error)
        fun loading(): ApiResult<Nothing> = Loading
    }

    inline fun <R> map(transform: (T) -> R): ApiResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    inline fun onError(action: (AppError) -> Unit): ApiResult<T> {
        if (this is Error) {
            action(error)
        }
        return this
    }

    inline fun onLoading(action: () -> Unit): ApiResult<T> {
        if (this is Loading) {
            action()
        }
        return this
    }

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
        is Loading -> null
    }

    fun errorOrNull(): AppError? = when (this) {
        is Success -> null
        is Error -> error
        is Loading -> null
    }

    fun isSuccess() = this is Success
    fun isError() = this is Error
    fun isLoading() = this is Loading

    fun getErrorMessage(): String = when (this) {
        is Success -> ""
        is Error -> error.getUserMessage()
        is Loading -> ""
    }
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (AppError) -> R,
        onLoading: () -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(error)
            is Loading -> onLoading()
        }
    }


}