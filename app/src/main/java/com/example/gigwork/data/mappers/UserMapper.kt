package com.example.gigwork.data.mappers

import com.example.gigwork.data.database.UserProfileEntity
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
        firebase_uid = id, // Use the same ID as firebase_uid for now
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
        photo = profilePhoto,
        dateOfBirth = dateOfBirth,
        gender = gender,
        currentLocation = currentLocation?.toDomain(),
        preferredLocation = preferredLocation?.toDomain(),
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
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
        photo = photo,
        dateOfBirth = dateOfBirth,
        gender = gender,
        currentLocation = currentLocation?.toDto(),
        preferredLocation = preferredLocation?.toDto(),
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
        profilePhoto = photo,
        companyName = companyName,
        companyFunction = companyFunction,
        staffCount = staffCount,
        yearlyTurnover = yearlyTurnover
    )
}
    fun UserProfileEntity.toDomain(): UserProfile {
        return UserProfile(
            id = id,
            userId = userId,
            name = name,
            photo = photo,
            dateOfBirth = dateOfBirth,
            gender = gender,
            currentLocation = null, // Parse from string if needed
            preferredLocation = null, // Parse from string if needed
            qualification = qualification,
            computerKnowledge = computerKnowledge,
            aadharNumber = aadharNumber,
            companyName = companyName,
            companyFunction = companyFunction,
            staffCount = staffCount,
            yearlyTurnover = yearlyTurnover
        )
    }
