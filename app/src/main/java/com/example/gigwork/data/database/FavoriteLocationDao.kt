// FavoriteLocationDao.kt
package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FavoriteLocationDao {
    /**
     * Insert a new favorite location
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteLocation(location: FavoriteLocationEntity): Long

    /**
     * Get all favorite locations for a user
     */
    @Query("SELECT * FROM favorite_locations WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getUserFavoriteLocations(userId: String): List<FavoriteLocationEntity>

    /**
     * Get all favorite locations
     */
    @Query("SELECT * FROM favorite_locations ORDER BY timestamp DESC")
    suspend fun getAllFavoriteLocations(): List<FavoriteLocationEntity>

    /**
     * Get favorite location by ID
     */
    @Query("SELECT * FROM favorite_locations WHERE id = :locationId")
    suspend fun getFavoriteLocationById(locationId: Long): FavoriteLocationEntity?

    /**
     * Delete a favorite location
     */
    @Query("DELETE FROM favorite_locations WHERE id = :locationId")
    suspend fun deleteFavoriteLocation(locationId: Long)

    /**
     * Delete all favorite locations for a user
     */
    @Query("DELETE FROM favorite_locations WHERE userId = :userId")
    suspend fun clearUserFavoriteLocations(userId: String)

    /**
     * Delete all favorite locations
     */
    @Query("DELETE FROM favorite_locations")
    suspend fun clearAllFavoriteLocations()

    /**
     * Check if a location is a favorite
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorite_locations WHERE userId = :userId AND state = :state AND district = :district LIMIT 1)")
    suspend fun isLocationFavorite(userId: String, state: String, district: String): Boolean

    /**
     * Get favorite location by state and district
     */
    @Query("SELECT * FROM favorite_locations WHERE userId = :userId AND state = :state AND district = :district LIMIT 1")
    suspend fun getFavoriteLocationByStateAndDistrict(userId: String, state: String, district: String): FavoriteLocationEntity?

    /**
     * Remove duplicate locations and keep the most recent
     */
    @Transaction
    suspend fun cleanupDuplicateLocations(userId: String) {
        val locations = getUserFavoriteLocations(userId)
        val uniqueLocations = mutableMapOf<Pair<String, String>, FavoriteLocationEntity>()

        // Keep only the most recent location for each state+district
        for (location in locations) {
            val key = Pair(location.state, location.district)
            val existing = uniqueLocations[key]

            if (existing == null || existing.timestamp < location.timestamp) {
                uniqueLocations[key] = location
            }
        }

        // Delete all and reinsert unique locations
        clearUserFavoriteLocations(userId)
        uniqueLocations.values.forEach { insertFavoriteLocation(it) }
    }

    /**
     * Get favorite locations by state
     */
    @Query("SELECT * FROM favorite_locations WHERE userId = :userId AND state = :state ORDER BY district")
    suspend fun getFavoriteLocationsByState(userId: String, state: String): List<FavoriteLocationEntity>

    /**
     * Get unique states from favorite locations
     */
    @Query("SELECT DISTINCT state FROM favorite_locations WHERE userId = :userId ORDER BY state")
    suspend fun getFavoriteLocationStates(userId: String): List<String>

    /**
     * Get count of favorite locations
     */
    @Query("SELECT COUNT(*) FROM favorite_locations WHERE userId = :userId")
    suspend fun getFavoriteLocationsCount(userId: String): Int
}