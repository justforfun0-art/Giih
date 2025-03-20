package com.example.gigwork.data.mappers

import com.example.gigwork.data.mappers.LocationDto
import com.example.gigwork.domain.models.Location

fun LocationDto.toDomain(): Location {
    return Location(
        latitude = latitude,
        longitude = longitude,
        address = address,
        pinCode = pinCode,  // Add the missing pinCode parameter
        state = state.orEmpty(),
        district = district.orEmpty()
    )
}

fun Location.toDto(): LocationDto {
    return LocationDto(
        latitude = latitude,
        longitude = longitude,
        address = address,
        pinCode = pinCode,  // Add the missing pinCode parameter
        state = state,
        district = district
    )
}