package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.UserApi
import com.example.gigwork.data.database.UserDao
import com.example.gigwork.data.database.UserEntity
import com.example.gigwork.data.database.UserProfileEntity
import com.example.gigwork.data.database.UserWithProfile
import com.example.gigwork.data.mappers.UserDto
import com.example.gigwork.data.mappers.UserProfileDto
import com.example.gigwork.data.mappers.toDto
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.gigwork.data.mappers.toDomain
import kotlinx.coroutines.flow.catch
import java.util.UUID

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val userDao: UserDao,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserRepository {

    companion object {
        private const val TAG = "UserRepository"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    override suspend fun getUser(userId: String): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)  // Start with loading state
        try {
            // Try to get from local database first
            val cachedUser = withContext(ioDispatcher) {
                userDao.getUser(userId)
            }

            if (cachedUser != null && !isCacheExpired(cachedUser.user.timestamp)) {
                logger.d(
                    tag = TAG,
                    message = "Returning cached user data",
                    additionalData = mapOf("user_id" to userId)
                )
                emit(ApiResult.Success(mapUserWithProfileToDomain(cachedUser)))
                return@flow
            }

            // Get from remote if cache is expired or not available
            try {
                val remoteUser = api.getUser(userId)

                // Save to local database
                val userEntity = UserEntity(
                    id = remoteUser.id,
                    name = remoteUser.name,
                    email = remoteUser.email,
                    phone = remoteUser.phone,
                    type = remoteUser.type,
                    timestamp = System.currentTimeMillis()
                )

                val profileEntity = remoteUser.profile?.let {
                    UserProfileEntity(
                        id = it.id,
                        userId = remoteUser.id,
                        name = it.name,
                        photo = it.photo,
                        dateOfBirth = it.dateOfBirth,
                        gender = it.gender,
                        currentLocation = it.currentLocation?.toString(),
                        preferredLocation = it.preferredLocation?.toString(),
                        qualification = it.qualification,
                        computerKnowledge = it.computerKnowledge == true,                        aadharNumber = it.aadharNumber,
                        companyName = it.companyName,
                        companyFunction = it.companyFunction,
                        staffCount = it.staffCount,
                        yearlyTurnover = it.yearlyTurnover
                    )
                }

                withContext(ioDispatcher) {
                    userDao.insertUser(userEntity)
                    profileEntity?.let { userDao.insertUserProfile(it) }
                }

                logger.i(
                    tag = TAG,
                    message = "Successfully fetched user from remote",
                    additionalData = mapOf("user_id" to userId)
                )

                emit(ApiResult.Success(mapUserDtoToDomain(remoteUser)))
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error fetching user from remote",
                    throwable = e,
                    additionalData = mapOf("user_id" to userId)
                )

                // If we have cached data, return it even if it's expired
                if (cachedUser != null) {
                    emit(ApiResult.Success(mapUserWithProfileToDomain(cachedUser)))
                } else {
                    emit(ApiResult.Error(
                        AppError.UnexpectedError(
                            message = e.message ?: "Failed to fetch user from remote",
                            cause = e
                        )
                    ))
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting user",
                throwable = e,
                additionalData = mapOf("user_id" to userId)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to get user",
                    cause = e
                )
            ))
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            // Validate profile
            if (profile.userId.isBlank()) {
                emit(ApiResult.Error(
                    AppError.ValidationError(
                        message = "User ID cannot be blank",
                        field = "userId"
                    )
                ))
                return@flow
            }

            // Update remote first
            try {
                val profileDto = UserProfileDto(
                    id = profile.id,
                    userId = profile.userId,
                    name = profile.name,
                    photo = profile.photo,
                    dateOfBirth = profile.dateOfBirth,
                    gender = profile.gender,
                    currentLocation = profile.currentLocation?.toDto(),
                    preferredLocation = profile.preferredLocation?.toDto(),
                    qualification = profile.qualification,
                    computerKnowledge = profile.computerKnowledge,
                    aadharNumber = profile.aadharNumber,
                    profilePhoto = profile.photo, // Note: If this is redundant with 'photo', you might need to adjust
                    companyName = profile.companyName,
                    companyFunction = profile.companyFunction,
                    staffCount = profile.staffCount,
                    yearlyTurnover = profile.yearlyTurnover
                )

                api.updateProfile(profileDto)

                // Update local database after remote update succeeds
                val profileEntity = UserProfileEntity(
                    id = profile.id,
                    userId = profile.userId,
                    name = profile.name,
                    photo = profile.photo,
                    dateOfBirth = profile.dateOfBirth,
                    gender = profile.gender,
                    currentLocation = profile.currentLocation?.toString(),
                    preferredLocation = profile.preferredLocation?.toString(),
                    qualification = profile.qualification,
                    computerKnowledge = profile.computerKnowledge == false,
                    aadharNumber = profile.aadharNumber,
                    companyName = profile.companyName,
                    companyFunction = profile.companyFunction,
                    staffCount = profile.staffCount,
                    yearlyTurnover = profile.yearlyTurnover
                )

                withContext(ioDispatcher) {
                    userDao.updateUserProfile(profileEntity)
                }

                logger.i(
                    tag = TAG,
                    message = "Successfully updated user profile",
                    additionalData = mapOf(
                        "user_id" to profile.userId,
                        "profile_id" to profile.id
                    )
                )

                emit(ApiResult.Success(Unit))
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error updating user profile on remote",
                    throwable = e,
                    additionalData = mapOf("user_id" to profile.userId)
                )
                emit(ApiResult.Error(
                    AppError.UnexpectedError(
                        message = e.message ?: "Failed to update user profile on remote",
                        cause = e
                    )
                ))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error updating user profile",
                throwable = e,
                additionalData = mapOf("user_id" to profile.userId)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to update user profile",
                    cause = e
                )
            ))
        }
    }

    override suspend fun createUser(user: User): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            logger.i(
                tag = TAG,
                message = "Creating new user",
                additionalData = mapOf(
                    "name" to user.name,
                    "type" to user.type.name,
                    "has_email" to (user.email != null).toString(),
                    "has_phone" to (user.phone != null).toString()
                )
            )

            // Get the password from secure storage
            val password = encryptedPreferences.getTemporaryPassword()
            if (password.isBlank()) {
                emit(ApiResult.Error(
                    AppError.ValidationError(
                        message = "No password provided for registration",
                        field = "password"
                    )
                ))
                return@flow
            }

            // Create user with auth in a single step
            val createdUser = try {
                val userDto = user.toDto()
                api.createUserWithAuth(userDto, password)
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error creating user with authentication",
                    throwable = e,
                    additionalData = mapOf("user_type" to user.type.name)
                )
                emit(ApiResult.Error(ExceptionMapper.map(e, "CREATE_USER_WITH_AUTH")))
                return@flow
            }

            // Clear the temporary password now that we're done with it
            encryptedPreferences.clearTemporaryPassword()

            // Save to local database
            try {
                val userEntity = UserEntity(
                    id = createdUser.id,
                    name = createdUser.name,
                    email = createdUser.email,
                    phone = createdUser.phone,
                    type = createdUser.type,
                    timestamp = System.currentTimeMillis()
                )

                val profileEntity = createdUser.profile?.let {
                    UserProfileEntity(
                        id = it.id,
                        userId = createdUser.id,
                        name = it.name,
                        photo = it.photo,
                        dateOfBirth = it.dateOfBirth,
                        gender = it.gender,
                        currentLocation = it.currentLocation?.toString(),
                        preferredLocation = it.preferredLocation?.toString(),
                        qualification = it.qualification,
                        computerKnowledge = it.computerKnowledge == false,
                        aadharNumber = it.aadharNumber,
                        companyName = it.companyName,
                        companyFunction = it.companyFunction,
                        staffCount = it.staffCount,
                        yearlyTurnover = it.yearlyTurnover
                    )
                }

                withContext(ioDispatcher) {
                    userDao.insertUser(userEntity)
                    profileEntity?.let { userDao.insertUserProfile(it) }
                }

                // Save user ID to preferences
                encryptedPreferences.saveUserId(createdUser.id)

                logger.i(
                    tag = TAG,
                    message = "Successfully created user",
                    additionalData = mapOf(
                        "user_id" to createdUser.id,
                        "user_type" to createdUser.type
                    )
                )

                emit(ApiResult.Success(mapUserDtoToDomain(createdUser)))
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error saving user to local database",
                    throwable = e
                )
                // Still return success since the remote operations succeeded
                emit(ApiResult.Success(mapUserDtoToDomain(createdUser)))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error creating user",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "CREATE_USER")))
        }
    }

    override suspend fun deleteUser(userId: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            // Delete from remote first
            try {
                api.deleteUser(userId)

                // Then delete from local database
                withContext(ioDispatcher) {
                    userDao.deleteUser(userId)
                    userDao.deleteUserProfile(userId)
                }

                logger.i(
                    tag = TAG,
                    message = "Successfully deleted user",
                    additionalData = mapOf("user_id" to userId)
                )

                emit(ApiResult.Success(Unit))
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error deleting user from remote",
                    throwable = e,
                    additionalData = mapOf("user_id" to userId)
                )
                emit(ApiResult.Error(
                    AppError.UnexpectedError(
                        message = e.message ?: "Failed to delete user from remote",
                        cause = e
                    )
                ))
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error deleting user",
                throwable = e,
                additionalData = mapOf("user_id" to userId)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to delete user",
                    cause = e
                )
            ))
        }
    }

    override suspend fun searchUsers(query: String): Flow<ApiResult<List<User>>> = flow {
        emit(ApiResult.Loading)

        try {
            // Implementation would depend on your API structure
            // This is a placeholder implementation
            emit(ApiResult.Error(
                AppError.BusinessError(
                    message = "Search users not implemented",
                    domain = "UserRepository"
                )
            ))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error searching users",
                throwable = e,
                additionalData = mapOf("query" to query)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to search users",
                    cause = e
                )
            ))
        }
    }

    override suspend fun userExists(userId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)

        try {
            val exists = withContext(ioDispatcher) {
                userDao.getUser(userId) != null
            }

            emit(ApiResult.Success(exists))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error checking if user exists",
                throwable = e,
                additionalData = mapOf("user_id" to userId)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to check if user exists",
                    cause = e
                )
            ))
        }
    }

    override suspend fun getUserByEmail(email: String): Flow<ApiResult<User>> = flow {
        emit(ApiResult.Loading)

        try {
            // Implementation would depend on your API structure
            // This is a placeholder implementation
            emit(ApiResult.Error(
                AppError.BusinessError(
                    message = "Get user by email not implemented",
                    domain = "UserRepository"
                )
            ))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting user by email",
                throwable = e,
                additionalData = mapOf("email" to email)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to get user by email",
                    cause = e
                )
            ))
        }
    }

    override suspend fun updateUserStatus(userId: String, status: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)

        try {
            // Implementation would depend on your API structure
            // This is a placeholder implementation
            emit(ApiResult.Error(
                AppError.BusinessError(
                    message = "Update user status not implemented",
                    domain = "UserRepository"
                )
            ))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error updating user status",
                throwable = e,
                additionalData = mapOf(
                    "user_id" to userId,
                    "status" to status
                )
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to update user status",
                    cause = e
                )
            ))
        }
    }

    override suspend fun getUsersByRole(role: String): Flow<ApiResult<List<User>>> = flow {
        emit(ApiResult.Loading)

        try {
            // Implementation would depend on your API structure
            // This is a placeholder implementation
            emit(ApiResult.Error(
                AppError.BusinessError(
                    message = "Get users by role not implemented",
                    domain = "UserRepository"
                )
            ))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting users by role",
                throwable = e,
                additionalData = mapOf("role" to role)
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to get users by role",
                    cause = e
                )
            ))
        }
    }
    override suspend fun getUserProfile(): Flow<ApiResult<UserProfile>> = flow {
        emit(ApiResult.Loading)
        val userId = getCurrentUserId()

        if (userId.isBlank()) {
            emit(ApiResult.Error(ExceptionMapper.map(
                IllegalStateException("User not logged in"),
                "GET_USER_PROFILE_NO_USER"
            )))
            return@flow
        }

        try {
            // Try remote first for fresh data
            val remoteProfile = try {
                api.getProfile(userId)?.toDomain() ?: createDefaultProfile(userId)
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error fetching remote profile",
                    throwable = e,
                    additionalData = mapOf("user_id" to userId)
                )
                null
            }

            // Handle remote result
            remoteProfile?.let { profile ->
                // Update cache
                withContext(ioDispatcher) {
                    userDao.insertUserProfile(profile.toEntity())
                }
                emit(ApiResult.Success(profile))
                return@flow
            }

            // Fallback to cached data
            val cachedProfile = withContext(ioDispatcher) {
                userDao.getUserProfile(userId)?.toDomain()
            }

            cachedProfile?.let {
                emit(ApiResult.Success(it))
            } ?: emit(ApiResult.Error(ExceptionMapper.map(
                IllegalStateException("Profile not found"),
                "GET_USER_PROFILE_NOT_FOUND"
            )))

        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting user profile",
                throwable = e,
                additionalData = mapOf("user_id" to userId)
            )
            // Exception will be handled in catch operator
            throw e
        }
    }.catch { cause ->
        emit(ApiResult.Error(ExceptionMapper.map(cause, "GET_USER_PROFILE_FLOW")))
    }

    private suspend fun createDefaultProfile(userId: String): UserProfile {
        val defaultProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "New User",
            photo = null,
            dateOfBirth = null,
            gender = null,
            currentLocation = null,
            preferredLocation = null,
            qualification = null,
            computerKnowledge = false,
            aadharNumber = null,
            companyName = null,
            companyFunction = null,
            staffCount = null,
            yearlyTurnover = null
        )

        try {
            api.createProfile(defaultProfile.toDto())
        } catch (e: Exception) {
            logger.w(
                tag = TAG,
                message = "Error creating default profile",
                throwable = e,
                additionalData = mapOf("user_id" to userId)
            )
        }

        return defaultProfile
    }

    // Add these extension functions
    private fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
        id = this.id,
        userId = this.userId,
        name = this.name,
        photo = this.photo,
        dateOfBirth = this.dateOfBirth,
        gender = this.gender,
        currentLocation = null, // Add proper parsing if needed
        preferredLocation = null,
        qualification = this.qualification,
        computerKnowledge = this.computerKnowledge,
        aadharNumber = this.aadharNumber,
        companyName = this.companyName,
        companyFunction = this.companyFunction,
        staffCount = this.staffCount,
        yearlyTurnover = this.yearlyTurnover
    )

    private fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
        id = this.id,
        userId = this.userId,
        name = this.name,
        photo = this.photo,
        dateOfBirth = this.dateOfBirth,
        gender = this.gender,
        currentLocation = this.currentLocation?.toString(),
        preferredLocation = this.preferredLocation?.toString(),
        qualification = this.qualification,
        computerKnowledge = this.computerKnowledge,
        aadharNumber = this.aadharNumber,
        companyName = this.companyName,
        companyFunction = this.companyFunction,
        staffCount = this.staffCount,
        yearlyTurnover = this.yearlyTurnover
    )

    override suspend fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }

    override suspend fun getUsersByIds(userIds: List<String>): Flow<ApiResult<Map<String, User>>> = flow {
        emit(ApiResult.Loading)

        try {
            val result = mutableMapOf<String, User>()

            // Get from local cache first
            val cachedUsers = withContext(ioDispatcher) {
                userIds.mapNotNull { userId ->
                    userDao.getUser(userId)?.let {
                        userId to mapUserWithProfileToDomain(it)
                    }
                }.toMap()
            }

            if (cachedUsers.isNotEmpty()) {
                result.putAll(cachedUsers)
            }

            // Get any missing users from remote
            val missingUserIds = userIds.filter { it !in cachedUsers.keys }
            if (missingUserIds.isNotEmpty()) {
                // Implementation would depend on your API structure
                // This is a simplified example assuming no batch API
                missingUserIds.forEach { userId ->
                    try {
                        val remoteUser = api.getUser(userId)
                        result[userId] = mapUserDtoToDomain(remoteUser)

                        // Cache the user
                        val userEntity = UserEntity(
                            id = remoteUser.id,
                            name = remoteUser.name,
                            email = remoteUser.email,
                            phone = remoteUser.phone,
                            type = remoteUser.type,
                            timestamp = System.currentTimeMillis()
                        )

                        val profileEntity = remoteUser.profile?.let {
                            UserProfileEntity(
                                id = it.id,
                                userId = remoteUser.id,
                                name = it.name,
                                photo = it.photo,
                                dateOfBirth = it.dateOfBirth,
                                gender = it.gender,
                                currentLocation = it.currentLocation?.toString(),
                                preferredLocation = it.preferredLocation?.toString(),
                                qualification = it.qualification,
                                computerKnowledge = it.computerKnowledge == false,
                                aadharNumber = it.aadharNumber,
                                companyName = it.companyName,
                                companyFunction = it.companyFunction,
                                staffCount = it.staffCount,
                                yearlyTurnover = it.yearlyTurnover
                            )
                        }

                        withContext(ioDispatcher) {
                            userDao.insertUser(userEntity)
                            profileEntity?.let { userDao.insertUserProfile(it) }
                        }
                    } catch (e: Exception) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching user from remote",
                            throwable = e,
                            additionalData = mapOf("user_id" to userId)
                        )
                        // Continue with other users
                    }
                }
            }

            emit(ApiResult.Success(result))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting users by IDs",
                throwable = e,
                additionalData = mapOf("user_ids" to userIds.joinToString())
            )
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to get users by IDs",
                    cause = e
                )
            ))
        }
    }

    // Helper methods

    private fun isCacheExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > CACHE_DURATION
    }

    private fun mapUserWithProfileToDomain(userWithProfile: UserWithProfile): User {
        val user = userWithProfile.user
        val profile = userWithProfile.profile

        return User(
            id = user.id,
            name = user.name,
            email = user.email,
            phone = user.phone,
            type = UserType.valueOf(user.type),
            profile = profile?.let { mapUserProfileEntityToDomain(it, user.id) }
        )
    }

    private fun mapUserProfileEntityToDomain(entity: UserProfileEntity, userId: String): UserProfile {
        return UserProfile(
            id = entity.id,
            userId = userId,
            name = entity.name,
            photo = entity.photo,
            dateOfBirth = entity.dateOfBirth,
            gender = entity.gender,
            currentLocation = null, // Would need to parse from string
            preferredLocation = null, // Would need to parse from string
            qualification = entity.qualification,
            computerKnowledge = entity.computerKnowledge,
            aadharNumber = entity.aadharNumber,
            companyName = entity.companyName,
            companyFunction = entity.companyFunction,
            staffCount = entity.staffCount,
            yearlyTurnover = entity.yearlyTurnover
        )
    }

    private fun mapUserDtoToDomain(dto: UserDto): User {
        return User(
            id = dto.id,
            name = dto.name,
            email = dto.email,
            phone = dto.phone,
            type = UserType.valueOf(dto.type),
            profile = dto.profile?.toDomain()
        )
    }
}