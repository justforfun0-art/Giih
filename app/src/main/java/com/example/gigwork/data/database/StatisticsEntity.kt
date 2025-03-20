// StatisticsEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for complex data in StatisticsEntity
 */
class StatisticsConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromDistributionMap(value: Map<Int, Int>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toDistributionMap(value: String): Map<Int, Int> {
        val mapType = object : TypeToken<Map<Int, Int>>() {}.type
        return gson.fromJson(value, mapType)
    }
}

@Entity(
    tableName = "job_statistics",
    indices = [
        Index(value = ["jobId"], unique = true)
    ]
)
@TypeConverters(StatisticsConverters::class)
data class StatisticsEntity(
    @PrimaryKey
    val id: String,
    val jobId: String,
    val viewsCount: Int,
    val applicationsCount: Int,
    val activeApplicationsCount: Int,
    val averageApplicantRating: Double,
    val lastUpdated: Long
)

/**
 * Entity for employer rating summaries
 */
@Entity(
    tableName = "employer_rating_summaries",
    indices = [
        Index(value = ["employerId"], unique = true)
    ]
)

@TypeConverters(StatisticsConverters::class)
data class EmployerRatingSummary(
    @PrimaryKey
    val employerId: String,
    val averageRating: Double,
    val totalRatings: Int,
    val responseRate: Double,
    val averageResponseTime: Long,
    val distribution: Map<Int, Int>,
    val timestamp: Long
)
