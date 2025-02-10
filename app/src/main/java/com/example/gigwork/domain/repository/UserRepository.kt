package com.example.gigwork.domain.repository

import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    /**
     * Get user details by ID
     * @param userId Unique identifier of the user
     * @return Flow of Result containing User data or error
     */
    suspend fun getUser(userId: String): Flow<Result<User>>

    /**
     * Update user profile information
     * @param profile UserProfile containing updated information
     * @return Flow of Result indicating success or failure
     */
    suspend fun updateUserProfile(profile: UserProfile): Flow<Result<Unit>>

    /**
     * Create a new user
     * @param user User object containing new user information
     * @return Flow of Result containing created User data or error
     */
    suspend fun createUser(user: User): Flow<Result<User>>

    /**
     * Delete an existing user
     * @param userId Unique identifier of the user to delete
     * @return Flow of Result indicating success or failure
     */
    suspend fun deleteUser(userId: String): Flow<Result<Unit>>

    /**
     * Search for users based on query
     * @param query Search query string
     * @return Flow of Result containing list of matching users or error
     */
    suspend fun searchUsers(query: String): Flow<Result<List<User>>>

    /**
     * Check if user exists
     * @param userId Unique identifier of the user
     * @return Flow of Result containing boolean indicating existence
     */
    suspend fun userExists(userId: String): Flow<Result<Boolean>>

    /**
     * Get user by email
     * @param email Email address of the user
     * @return Flow of Result containing User data or error
     */
    suspend fun getUserByEmail(email: String): Flow<Result<User>>

    /**
     * Update user status (active/inactive/suspended)
     * @param userId Unique identifier of the user
     * @param status New status to set
     * @return Flow of Result indicating success or failure
     */
    suspend fun updateUserStatus(userId: String, status: String): Flow<Result<Unit>>

    /**
     * Get list of users by role
     * @param role Role to filter by
     * @return Flow of Result containing list of users with specified role
     */
    suspend fun getUsersByRole(role: String): Flow<Result<List<User>>>

    /**
     * Batch get users by IDs
     * @param userIds List of user IDs to fetch
     * @return Flow of Result containing map of userIds to User objects
     */
    suspend fun getUsersByIds(userIds: List<String>): Flow<Result<Map<String, User>>>
}