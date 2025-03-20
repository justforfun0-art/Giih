package com.example.gigwork.data.database

// data/db/entity/UserEntityMapper.kt

import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.models.UserType

fun UserWithProfile.toDomain(): User {
    return User(
        id = user.id,
        name = user.name,
        email = user.email,
        phone = user.phone,
        type = UserType.valueOf(user.type),
        profile = profile?.toDomain()
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
        currentLocation = null, // These would need to be stored separately
        preferredLocation = null,
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
        companyName = companyName,
        companyFunction = companyFunction,
        staffCount = staffCount,
        yearlyTurnover = yearlyTurnover
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        phone = phone,
        type = type.name
    )
}

fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        userId = userId,
        name = name,
        photo = photo,
        dateOfBirth = dateOfBirth,
        gender = gender,
        currentLocation = currentLocation?.toString(),  // Add this
        preferredLocation = preferredLocation?.toString(),  // Add this
        qualification = qualification,
        computerKnowledge = computerKnowledge,
        aadharNumber = aadharNumber,
        companyName = companyName,
        companyFunction = companyFunction,
        staffCount = staffCount,
        yearlyTurnover = yearlyTurnover
    )
}