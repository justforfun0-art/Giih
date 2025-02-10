// data/database/entity/JobDraftEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.gigwork.data.database.LocationConverter
import com.example.gigwork.domain.models.Location
import com.example.gigwork.presentation.states.JobDraft
import java.time.Instant

@Entity(tableName = "job_drafts")
@TypeConverters(LocationConverter::class)
data class JobDraftEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val salary: Double,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String,
    val location: Location,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = Instant.now().toEpochMilli(),
    val lastModified: Long
)

// Extension functions for mapping
fun JobDraftEntity.toDomain() = JobDraft(
    id = id,
    title = title,
    description = description,
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    location = location,
    lastModified = lastModified
)

fun JobDraft.toEntity() = JobDraftEntity(
    id = id,
    title = title,
    description = description,
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    location = location,
    lastModified = lastModified
)