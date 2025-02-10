package com.example.gigwork.domain.usecase.user


import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.UserRepository  // Changed to domain repository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class CreateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(
        userId: String,
        profile: UserProfile
    ): Flow<com.example.gigwork.util.Result<Unit>> {  // Changed return type to match repository
        // Validate input
        require(userId.isNotBlank()) { "User ID cannot be empty" }

        // Create profile with userId
        val completeProfile = profile.copy(userId = userId)

        // Validate profile data
        validateEmployeeProfile(completeProfile)

        // Update profile in repository
        return repository.updateUserProfile(completeProfile)
    }

    private fun validateEmployeeProfile(profile: UserProfile) {
        requireNotNull(profile.dateOfBirth) { "Date of birth is required" }
        requireNotNull(profile.gender) { "Gender is required" }
        requireNotNull(profile.currentLocation) { "Current location is required" }
        requireNotNull(profile.qualification) { "Qualification is required" }
        requireNotNull(profile.computerKnowledge) { "Computer knowledge information is required" }
    }
}