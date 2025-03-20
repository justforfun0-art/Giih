package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.repository.UserRepository
import io.github.jan.supabase.auth.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(id: String): Flow<ApiResult<User>> {
        require(id.isNotBlank()) { "User ID cannot be empty" }

        return flow {
            emit(ApiResult.Loading)

            try {
                val user = repository.getUser(id).collect { result ->
                    when (result) {
                        is ApiResult.Success -> emit(ApiResult.Success(result.data))
                        is ApiResult.Error -> emit(result)
                        is ApiResult.Loading -> emit(ApiResult.Loading)
                    }
                }
            } catch (e: Throwable) {
                emit(ApiResult.Error(
                    AppError.UnexpectedError(
                        message = e.message ?: "Failed to get user",
                        cause = e
                    )
                ))
            }
        }
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Flow<ApiResult<User>> {
        val currentUserId = userRepository.getCurrentUserId()

        return if (currentUserId.isNotBlank()) {
            getUserUseCase(currentUserId)
        } else {
            flow {
                emit(ApiResult.Error(
                    AppError.SecurityError(
                        message = "No user is currently logged in",
                        securityDomain = "authentication"
                    )
                ))
            }
        }
    }
}