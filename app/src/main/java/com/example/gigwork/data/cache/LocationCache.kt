package com.example.gigwork.data.cache

import android.content.Context
import androidx.room.*
import com.example.gigwork.core.error.model.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Entity(tableName = "location_cache")
data class LocationCacheEntity(
    @PrimaryKey val key: String,
    val data: String,
    val timestamp: Long
)

@Dao
interface LocationCacheDao {
    @Query("SELECT * FROM location_cache WHERE key = :key")
    suspend fun get(key: String): LocationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocationCacheEntity)

    @Query("DELETE FROM location_cache WHERE timestamp < :timestamp")
    suspend fun deleteOld(timestamp: Long)

    @Query("DELETE FROM location_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM location_cache")
    suspend fun getCount(): Int

    @Query("SELECT * FROM location_cache ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<LocationCacheEntity>>
}

@Database(entities = [LocationCacheEntity::class], version = 1, exportSchema = false)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationCacheDao(): LocationCacheDao

    companion object {
        const val DATABASE_NAME = "location_cache.db"
    }
}

@Singleton
class LocationCache @Inject constructor(
    @ApplicationContext context: Context
) {
    private val db = Room.databaseBuilder(
        context,
        LocationDatabase::class.java,
        LocationDatabase.DATABASE_NAME
    )
        .fallbackToDestructiveMigration()
        .build()

    private val dao = db.locationCacheDao()

    companion object {
        private const val DEFAULT_CACHE_DURATION = 24 * 60 * 60 * 1000L // 24 hours
        private const val TAG = "LocationCache"
    }

    /**
     * Get cached data for a key
     */
    suspend fun get(key: String): List<String>? {
        try {
            val cached = dao.get(key) ?: return null
            if (isCacheValid(cached.timestamp)) {
                return cached.data.split(",")
            }
            return null
        } catch (e: Exception) {
            throw AppError.DatabaseError(
                message = "Failed to read from cache: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = "read"
            )
        }
    }

    /**
     * Store data in cache
     */
    suspend fun put(key: String, data: List<String>, duration: Long = DEFAULT_CACHE_DURATION) {
        try {
            dao.insert(
                LocationCacheEntity(
                    key = key,
                    data = data.joinToString(","),
                    timestamp = System.currentTimeMillis()
                )
            )
            cleanOldCache()
        } catch (e: Exception) {
            throw AppError.DatabaseError(
                message = "Failed to write to cache: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = "write"
            )
        }
    }

    /**
     * Check if cached data is still valid
     */
    private fun isCacheValid(timestamp: Long, duration: Long = DEFAULT_CACHE_DURATION): Boolean {
        return System.currentTimeMillis() - timestamp < duration
    }

    /**
     * Clean old cached data
     */
    private suspend fun cleanOldCache() {
        val threshold = System.currentTimeMillis() - DEFAULT_CACHE_DURATION
        try {
            dao.deleteOld(threshold)
        } catch (e: Exception) {
            throw AppError.DatabaseError(
                message = "Failed to clean cache: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = "clean"
            )
        }
    }

    /**
     * Clear all cached data
     */
    suspend fun clear() {
        try {
            dao.clearAll()
        } catch (e: Exception) {
            throw AppError.DatabaseError(
                message = "Failed to clear cache: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = "clear"
            )
        }
    }

    /**
     * Get cache status as flow
     */
    fun getCacheStatus(): Flow<CacheStatus> = flow {
        try {
            val count = dao.getCount()
            emit(CacheStatus(
                itemCount = count,
                lastUpdated = getLastUpdateTime()
            ))
        } catch (e: Exception) {
            throw AppError.DatabaseError(
                message = "Failed to get cache status: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = "status"
            )
        }
    }

    private suspend fun getLastUpdateTime(): Long {
        return dao.getAllFlow().collect { entities ->
            entities.maxOfOrNull { it.timestamp } ?: 0L
        }
    }

    data class CacheStatus(
        val itemCount: Int,
        val lastUpdated: Long
    )
}