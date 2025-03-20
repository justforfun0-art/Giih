package com.example.gigwork.data.mappers

import com.example.gigwork.data.models.JobDto
import com.example.gigwork.domain.models.Job

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
        createdAt = createdAt,
        updatedAt = createdAt,
        lastModified = createdAt,
        company = ""
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