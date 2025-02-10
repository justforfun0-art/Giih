// data/database/entity/JobEntityMappers.kt
package com.example.gigwork.data.database

import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location

object JobEntityMapper {
    fun JobEntity.toDomainModel(): Job {
        return Job(
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
    }

    fun Job.toEntityModel(): JobEntity {
        return JobEntity(
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
    }
}