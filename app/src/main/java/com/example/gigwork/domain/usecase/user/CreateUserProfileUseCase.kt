package com.example.gigwork.domain.usecase.user

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.core.result.toAppError
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.UserRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        profile: UserProfile
    ): Flow<ApiResult<Unit>> {
        // Validate input
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        // Create profile with userId
        val completeProfile = profile.copy(userId = userId)

        // Validate profile data
        validateEmployeeProfile(completeProfile)

        // Update profile in repository
        return repository.updateUserProfile(completeProfile)
            .map { ApiResult.Success(Unit) }
            .catch { e -> ApiResult.Error(e.toAppError()) }
            .flowOn(Dispatchers.IO)
    }

    private fun validateEmployeeProfile(profile: UserProfile) {
        requireNotNull(profile.dateOfBirth) { "Date of birth is required" }
        requireNotNull(profile.name) { "Name is required" }
        requireNotNull(profile.gender) { "Gender is required" }
        requireNotNull(profile.currentLocation) { "Current location is required" }
        requireNotNull(profile.qualification) { "Qualification is required" }
        requireNotNull(profile.computerKnowledge) { "Computer knowledge information is required" }
    }
}