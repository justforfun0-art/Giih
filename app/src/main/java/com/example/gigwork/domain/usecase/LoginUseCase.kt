package com.example.gigwork.domain.usecase

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository  // Interface, not implementation
) {
    data class Params(
        val identifier: String,
        val password: String,
        val isEmployer: Boolean,
        val rememberMe: Boolean
    )

    suspend operator fun invoke(params: Params): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            // If login returns Flow<ApiResult<User>>, we need to collect and re-emit
            authRepository.login(
                identifier = params.identifier,
                password = params.password,
                isEmployer = params.isEmployer,
                rememberMe = params.rememberMe
            ).collect { result ->
                emit(result)  // Forward the result from the repository flow
            }
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "An unexpected error occurred during login",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        }
    }
}