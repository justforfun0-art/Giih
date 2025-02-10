// data/models/JobDto.kt
package com.example.gigwork.data.models

import kotlinx.serialization.Serializable

@Serializable
data class JobDto(
    val id: String,
    val title: String,
    val description: String,
    val salary: Double,
    val employerId: String,
    val location: LocationDto,
    val status: String,
    val createdAt: String
)

@Serializable
data class LocationDto(
    val state: String,
    val district: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// Add mapper function to convert to domain model
fun JobDto.toDomainModel() = Job(
    id = id,
    title = title,
    description = description,
    salary = salary,
    location = location.toDomainModel(),
    status = status,
    createdAt = createdAt
)