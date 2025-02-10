// data/mappers/JobMapper.kt
package com.example.gigwork.data.mappers

import com.example.gigwork.data.models.JobDto
import com.example.gigwork.data.models.LocationDto
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location

fun JobDto.toDomain(): Job {
    return Job(
        id = id,
        title = title,
        description = description,
        employerId = employerId,
        location = location.toDomain(),
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        status = status,
        createdAt = createdAt
    )
}

fun Job.toDto(): JobDto {
    return JobDto(
        id = id,
        title = title,
        description = description,
        employerId = employerId,
        location = location.toDto(),
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        status = status,
        createdAt = createdAt
    )
}

fun LocationDto.toDomain(): Location {
    return Location(
        state = state,
        district = district,
        latitude = latitude,
        longitude = longitude
    )
}

fun Location.toDto(): LocationDto {
    return LocationDto(
        state = state,
        district = district,
        latitude = latitude,
        longitude = longitude
    )
}
