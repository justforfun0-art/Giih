package com.example.gigwork.domain.usecase.user

import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart

class GetUserUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(id: String): Flow<Result<User>> {
        require(id.isNotBlank()) { "User ID cannot be empty" }

        return repository.getUser(id)
            .onStart {
                emit(Result.Loading)
            }
            .catch { throwable ->
                // Changed from exception to throwable to match the Result.Error type
                emit(Result.Error(throwable))
            }
    }
}