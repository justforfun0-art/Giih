package com.example.gigwork.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.gigwork.data.database.dao.JobDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

@Database(
    entities = [
        // User related entities
        UserEntity::class,
        UserProfileEntity::class,

        // Job related entities
        JobEntity::class,
        JobDraftEntity::class,

        // Location related entities
        LocationEntity::class,
        FavoriteLocationEntity::class,

        // Application and interaction entities
        ApplicationEntity::class,
        BookmarkEntity::class,

        // Rating and statistics entities
        RatingEntity::class,
        JobRatingEntity::class,
        StatisticsEntity::class,
        EmployerRatingSummary::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    LocationConverter::class,
    DateConverter::class,
    StringListConverter::class,
    StatisticsConverters::class
)
abstract class AppDatabase : RoomDatabase() {
    // DAOs
    abstract fun jobDao(): JobDao
    abstract fun jobDraftDao(): JobDraftDao
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun favoriteLocationDao(): FavoriteLocationDao
    abstract fun applicationDao(): ApplicationDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun ratingDao(): RatingDao
    abstract fun statisticsDao(): StatisticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gigwork_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Converter for Location object

class LocationConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromLocation(location: com.example.gigwork.domain.models.Location?): String {
        return if (location == null) "" else gson.toJson(location)
    }

    @TypeConverter
    fun toLocation(value: String): com.example.gigwork.domain.models.Location? {
        return if (value.isEmpty()) null
        else gson.fromJson(value, com.example.gigwork.domain.models.Location::class.java)
    }
}
 */
/**
 * Converter for Date objects
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Converter for List<String>
 */
class StringListConverter {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList()
        else value.split(",")
    }
}

/**
 * Converter for Map<Int, Int> used in rating statistics
 */
