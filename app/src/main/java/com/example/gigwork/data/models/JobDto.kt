@file:OptIn(InternalSerializationApi::class)

package com.example.gigwork.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi
import com.example.gigwork.data.mappers.LocationDto

@Serializable
data class JobDto(
    val id: String,
    val title: String,
    val description: String,
    val salary: Double,
    val employerId: String,
    val location: LocationDto,
    val status: String,
    val createdAt: String,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String
)