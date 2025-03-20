// LocationDao.kt
package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LocationDao {
    /**
     * Insert a new location
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    /**
     * Get the most recent location for a user
     */
    @Query("SELECT * FROM user_locations WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getUserLastLocation(userId: String): LocationEntity?

    /**
     * Get recent locations for a user
     */
    @Query("SELECT * FROM user_locations WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getUserLocations(userId: String, limit: Int): List<LocationEntity>

    /**
     * Delete old locations for a user, keeping only recent ones
     */
    @Query("DELETE FROM user_locations WHERE userId = :userId AND timestamp < :oldestTimestamp")
    suspend fun deleteOldLocations(userId: String, oldestTimestamp: Long)

    /**
     * Clear all locations for a user
     */
    @Query("DELETE FROM user_locations WHERE userId = :userId")
    suspend fun clearUserLocations(userId: String)

    /**
     * Get all unique states where the user has been
     */
    @Query("SELECT DISTINCT state FROM user_locations WHERE userId = :userId")
    suspend fun getUserStates(userId: String): List<String>

    /**
     * Get all unique districts in a state where the user has been
     */
    @Query("SELECT DISTINCT district FROM user_locations WHERE userId = :userId AND state = :state")
    suspend fun getUserDistricts(userId: String, state: String): List<String>

    /**
     * Insert a location and delete old ones, keeping only the latest [maxLocations]
     */
    @Transaction
    suspend fun insertLocationAndPrune(location: LocationEntity, maxLocations: Int) {
        insertLocation(location)

        // Get the timestamp of the oldest location to keep
        val locations = getUserLocations(location.userId, maxLocations + 1)
        if (locations.size > maxLocations) {
            val oldestToKeep = locations[maxLocations - 1].timestamp
            deleteOldLocations(location.userId, oldestToKeep)
        }
    }

    /**
     * Get latest location for a list of users
     */
    @Query("SELECT * FROM user_locations WHERE userId IN (:userIds) GROUP BY userId HAVING MAX(timestamp)")
    suspend fun getLastLocationsForUsers(userIds: List<String>): List<LocationEntity>
}