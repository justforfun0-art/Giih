package com.example.gigwork.domain.models

data class Job(
    val applicationDeadline: Long? = null,
    val id: String,
    val title: String,
    val description: String,
    val employerId: String,
    val location: Location,
    val salary: Double,
    val salaryUnit: String,
    val workDuration: Int,
    val workDurationUnit: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastModified: String,
    val company: String
)

data class Location(
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val pinCode: String?,
    val state: String,
    val district: String
)