// ApplicationEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "job_applications",
    indices = [
        Index(value = ["userId", "jobId"], unique = true),
        Index(value = ["jobId"]),
        Index(value = ["status"])
    ]
)
data class ApplicationEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val jobId: String,
    val status: String, // Using string to match enum names in JobApplicationRepository.ApplicationStatus
    val attachments: String, // Comma-separated list of attachment URLs
    val appliedAt: Long,
    val updatedAt: Long
)
