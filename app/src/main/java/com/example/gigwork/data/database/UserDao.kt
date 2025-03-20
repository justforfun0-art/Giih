package com.example.gigwork.data.database

import androidx.room.*
import com.example.gigwork.data.database.UserEntity
import com.example.gigwork.data.database.UserProfileEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE userId = :userId")
    suspend fun getUserProfile(userId: String): UserProfileEntity?

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUser(userId: String): UserWithProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Transaction
    suspend fun insertUserWithProfile(user: UserEntity, profile: UserProfileEntity?) {
        insertUser(user)
        profile?.let { insertUserProfile(it) }
    }

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)

    @Query("DELETE FROM user_profiles WHERE userId = :userId")
    suspend fun deleteUserProfile(userId: String)

    @Query("SELECT timestamp FROM users WHERE id = :userId")
    suspend fun getUserTimestamp(userId: String): Long?
}