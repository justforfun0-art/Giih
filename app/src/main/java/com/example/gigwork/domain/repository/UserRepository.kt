package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Get user details by ID
     * @param userId Unique identifier of the user
     * @return Flow of ApiResult containing User data or error
     */
    suspend fun getUser(userId: String): Flow<ApiResult<User>>

    /**
     * Update user profile information
     * @param profile UserProfile containing updated information
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun updateUserProfile(profile: UserProfile): Flow<ApiResult<Unit>>

    /**
     * Create a new user
     * @param user User object containing new user information
     * @return Flow of ApiResult containing created User data or error
     */
    suspend fun createUser(user: User): Flow<ApiResult<User>>

    /**
     * Delete an existing user
     * @param userId Unique identifier of the user to delete
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun deleteUser(userId: String): Flow<ApiResult<Unit>>

    /**
     * Search for users based on query
     * @param query Search query string
     * @return Flow of ApiResult containing list of matching users or error
     */
    suspend fun searchUsers(query: String): Flow<ApiResult<List<User>>>

    /**
     * Check if user exists
     * @param userId Unique identifier of the user
     * @return Flow of ApiResult containing boolean indicating existence
     */
    suspend fun userExists(userId: String): Flow<ApiResult<Boolean>>

    /**
     * Get user by email
     * @param email Email address of the user
     * @return Flow of ApiResult containing User data or error
     */
    suspend fun getUserByEmail(email: String): Flow<ApiResult<User>>

    /**
     * Update user status (active/inactive/suspended)
     * @param userId Unique identifier of the user
     * @param status New status to set
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun updateUserStatus(userId: String, status: String): Flow<ApiResult<Unit>>

    /**
     * Get list of users by role
     * @param role Role to filter by
     * @return Flow of ApiResult containing list of users with specified role
     */
    suspend fun getUsersByRole(role: String): Flow<ApiResult<List<User>>>

    /**
     * Get current user's profile
     * @return Flow of ApiResult containing UserProfile
     */
    suspend fun getUserProfile(): Flow<ApiResult<UserProfile>>

    /**
     * Get current logged-in user's ID
     * @return User ID as a string
     */
    suspend fun getCurrentUserId(): String

    /**
     * Batch get users by IDs
     * @param userIds List of user IDs to fetch
     * @return Flow of ApiResult containing map of userIds to User objects
     */
    suspend fun getUsersByIds(userIds: List<String>): Flow<ApiResult<Map<String, User>>>
}