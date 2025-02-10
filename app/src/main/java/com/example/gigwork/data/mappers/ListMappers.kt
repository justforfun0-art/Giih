// data/mappers/ListMappers.kt
package com.example.gigwork.data.mappers

fun List<JobDto>.toDomain(): List<Job> = map { it.toDomain() }
fun List<Job>.toDto(): List<JobDto> = map { it.toDto() }

fun List<UserDto>.toDomain(): List<User> = map { it.toDomain() }
fun List<User>.toDto(): List<UserDto> = map { it.toDto() }