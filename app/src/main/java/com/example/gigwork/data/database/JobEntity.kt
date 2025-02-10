// data/database/entity/JobEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val employerId: String,
    val salary: Double,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String,
    val status: String,
    val createdAt: String,
    val locationstate: String,
    val locationdistrict: String,
    val locationlatitude: Double?,
    val locationlongitude: Double?,
    val category: String? = null
)


fun JobEntity.toDomain() = Job(
    id = id,
    title = title,
    description = description,
    employerId = employerId,
    location = Location(
        state = locationstate,
        district = locationdistrict,
        latitude = locationlatitude,
        longitude = locationlongitude
    ),
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    status = status,
    createdAt = createdAt
)

fun Job.toEntity() = JobEntity(
    id = id,
    title = title,
    description = description,
    employerId = employerId,
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    status = status,
    createdAt = createdAt,
    locationstate = location.state,
    locationdistrict = location.district,
    locationlatitude = location.latitude,
    locationlongitude = location.longitude
)