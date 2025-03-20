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
    val updatedAt: String,
    val lastModified: String,
    val locationstate: String,
    val locationdistrict: String,
    val locationlatitude: Double?,
    val locationlongitude: Double?,
    val locationaddress: String?,
    val locationpinCode: String? = null,
    val company: String = "",
    val category: String? = null,
    val applicationDeadline: Long? = null
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
        longitude = locationlongitude,
        address = locationaddress,
        pinCode = locationpinCode
    ),
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastModified = lastModified,
    company = company,
    applicationDeadline = applicationDeadline
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
    updatedAt = updatedAt,
    lastModified = lastModified,
    locationstate = location.state,
    locationdistrict = location.district,
    locationlatitude = location.latitude,
    locationlongitude = location.longitude,
    locationaddress = location.address,
    locationpinCode = location.pinCode,
    company = company,
    applicationDeadline = applicationDeadline,
    category = null // Optional field
)