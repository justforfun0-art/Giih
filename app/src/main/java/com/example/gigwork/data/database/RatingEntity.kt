// RatingEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employer_ratings",
    indices = [
        Index(value = ["employerId"]),
        Index(value = ["userId", "employerId"], unique = true)
    ]
)
data class RatingEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val employerId: String,
    val rating: Float,
    val review: String,
    val timestamp: Long
)

/**
 * Entity for job ratings
 */
@Entity(
    tableName = "job_ratings",
    indices = [
        Index(value = ["jobId"]),
        Index(value = ["userId", "jobId"], unique = true)
    ]
)
data class JobRatingEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val jobId: String,
    val employerId: String,
    val rating: Float,
    val review: String,
    val timestamp: Long
)