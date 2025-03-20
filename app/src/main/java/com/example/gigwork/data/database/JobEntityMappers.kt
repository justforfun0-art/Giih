package com.example.gigwork.data.database

import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.Location

class JobEntityMapper {
    companion object {
        fun mapToDomain(entity: JobEntity): Job {
            return Job(
                id = entity.id,
                title = entity.title,
                description = entity.description,
                employerId = entity.employerId,
                location = Location(
                    state = entity.locationstate,
                    district = entity.locationdistrict,
                    latitude = entity.locationlatitude,
                    longitude = entity.locationlongitude,
                    address = entity.locationaddress,
                    pinCode = null
                ),
                salary = entity.salary,
                salaryUnit = entity.salaryUnit,
                workDuration = entity.workDuration,
                workDurationUnit = entity.workDurationUnit,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt ?: entity.createdAt,
                lastModified = entity.lastModified ?: entity.createdAt,
                company = "",
                applicationDeadline = null
            )
        }

        fun mapToEntity(domain: Job): JobEntity {
            return JobEntity(
                id = domain.id,
                title = domain.title,
                description = domain.description,
                employerId = domain.employerId,
                salary = domain.salary,
                salaryUnit = domain.salaryUnit,
                workDuration = domain.workDuration,
                workDurationUnit = domain.workDurationUnit,
                status = domain.status,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                lastModified = domain.lastModified,
                locationstate = domain.location.state,
                locationdistrict = domain.location.district,
                locationlatitude = domain.location.latitude,
                locationlongitude = domain.location.longitude,
                locationaddress = domain.location.address,
                category = null
            )
        }
    }
}