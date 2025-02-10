package com.example.gigwork.domain.models

data class Job(
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
    val createdAt: String
)

data class Location(
    val state: String,
    val district: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)