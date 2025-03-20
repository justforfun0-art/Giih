// data/mappers/ListMappers.kt
package com.example.gigwork.data.mappers
import com.example.gigwork.domain.models.User
import com.example.gigwork.data.models.JobDto
//import com.example.gigwork.data.mappers.UserDto
import com.example.gigwork.domain.models.Job

@JvmName("jobListToDomain")
fun List<JobDto>.toDomain(): List<Job> = map { it.toDomain() }

@JvmName("userListToDomain")
fun List<UserDto>.toDomain(): List<User> = map { it.toDomain() }

@JvmName("jobListToDto")
fun List<Job>.toDto(): List<JobDto> = map { it.toDto() }

@JvmName("userListToDto")
fun List<User>.toDto(): List<UserDto> = map { it.toDto() }