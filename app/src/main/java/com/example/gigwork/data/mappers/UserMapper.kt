// data/mappers/UserMapper.kt
package com.example.gigwork.data.mappers

import com.example.gigwork.data.models.UserDto
import com.example.gigwork.data.models.UserProfileDto
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.models.UserType

fun UserDto.toDomain(): User {
    return User(
        id = id,
        name = name,
        email = email,
        phone = phone,
        type = UserType.valueOf(type),
        profile = profile?.toDomain()
    )
}

fun User.toDto(): UserDto {
    return UserDto(
        id = id,
        name = name,
        email = email,
        phone = phone,
        type = type.name,
        profile = profile?.toDto()
    )
}

fun UserProfileDto.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        userId = userId,
        name = name,
        dateOfBirth = dateOfBirth,
        gender = gender,
        currentLocation = currentLocation.toDomain(),
        preferredLocation = preferredLocation?.toDomain(),
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
        profilePhoto = profilePhoto,
        companyName = companyName,
        companyFunction = companyFunction,
        staffCount = staffCount,
        yearlyTurnover = yearlyTurnover
    )
}

fun UserProfile.toDto(): UserProfileDto {
    return UserProfileDto(
        id = id,
        userId = userId,
        name = name,
        dateOfBirth = dateOfBirth,
        gender = gender,
        currentLocation = currentLocation.toDto(),
        preferredLocation = preferredLocation?.toDto(),
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
        profilePhoto = profilePhoto,
        companyName = companyName,
        companyFunction = companyFunction,
        staffCount = staffCount,
        yearlyTurnover = yearlyTurnover
    )
}
