package com.example.gigwork.data.cache

import android.content.Context
import androidx.room.*
import com.example.gigwork.core.error.model.AppError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

// Entity
@Entity(tableName = "location_cache")
data class LocationCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val key: String,
    val data: String,
    val timestamp: Long
)

// DAO
@Dao
interface LocationCacheDao {
    @Query("SELECT * FROM location_cache WHERE cache_key = :key")
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

    @Query("SELECT MAX(timestamp) FROM location_cache")
    suspend fun getLastUpdateTime(): Long?
}

// Database
@Database(entities = [LocationCacheEntity::class], version = 1, exportSchema = false)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationCacheDao(): LocationCacheDao

    companion object {
        const val DATABASE_NAME = "location_cache.db"
    }
}

// Cache Implementation
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

    suspend fun get(key: String): List<String>? {
        return try {
            val cached = dao.get(key) ?: return null
            if (isCacheValid(cached.timestamp)) {
                cached.data.split(",")
            } else {
                null
            }
        } catch (e: Exception) {
            handleDatabaseError(e, "Failed to read from cache", "read")
        }
    }

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
            handleDatabaseError(e, "Failed to write to cache", "write")
        }
    }

    private fun isCacheValid(timestamp: Long, duration: Long = DEFAULT_CACHE_DURATION): Boolean {
        return System.currentTimeMillis() - timestamp < duration
    }

    private suspend fun cleanOldCache() {
        val threshold = System.currentTimeMillis() - DEFAULT_CACHE_DURATION
        try {
            dao.deleteOld(threshold)
        } catch (e: Exception) {
            handleDatabaseError(e, "Failed to clean cache", "clean")
        }
    }

    suspend fun clear() {
        try {
            dao.clearAll()
        } catch (e: Exception) {
            handleDatabaseError(e, "Failed to clear cache", "clear")
        }
    }

    fun getCacheStatus(): Flow<CacheStatus> = flow {
        try {
            val count = dao.getCount()
            val lastUpdated = dao.getLastUpdateTime() ?: 0L
            emit(CacheStatus(
                itemCount = count,
                lastUpdated = lastUpdated
            ))
        } catch (e: Exception) {
            handleDatabaseError(e, "Failed to get cache status", "status")
        }
    }

    data class CacheStatus(
        val itemCount: Int,
        val lastUpdated: Long
    )

    private fun handleDatabaseError(e: Exception, message: String, operation: String): Nothing {
        val error = when (e) {
            is AppError -> e
            else -> AppError.DatabaseError(
                message = "$message: ${e.message}",
                cause = e,
                entity = "location_cache",
                operation = operation
            )
        }
        throw RuntimeException(error.message, error.cause)
    }
}