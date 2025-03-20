package com.example.gigwork.data.mappers

import com.example.gigwork.data.database.JobDraftEntity
import com.example.gigwork.domain.models.Job
import com.example.gigwork.presentation.states.JobDraft
import com.example.gigwork.domain.models.Location

// Entity to Domain model conversion
fun JobDraftEntity.toDomain(): JobDraft {
    return JobDraft(
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
}

// Domain model to Entity conversion
fun JobDraft.toEntity(): JobDraftEntity {
    return JobDraftEntity(
        id = id,
        title = title,
        description = description,
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        location = location,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastModified = lastModified
    )
}

// Convert JobDraft to Job domain model if needed
fun JobDraft.toJob(employerId: String): Job {
    return Job(
        id = id,
        title = title,
        description = description,
        employerId = employerId,
        location = location,
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        status = "draft",
        createdAt = System.currentTimeMillis().toString(),
        updatedAt = System.currentTimeMillis().toString(),
        lastModified =System.currentTimeMillis().toString(),
        company = ""
    )
}