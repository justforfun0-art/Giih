package com.example.gigwork.data.mappers

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserDto(
    val id: String,
    val firebase_uid: String, // This is the Firebase UID
    val name: String,
    val email: String?,
    val phone: String?,
    val type: String,
    val profile: UserProfileDto? = null
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserProfileDto(
    val id: String,
    val userId: String,
    val name: String,
    val photo: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val currentLocation: LocationDto?,
    val preferredLocation: LocationDto?,
    val qualification: String?,
    val computerKnowledge: Boolean?,
    val aadharNumber: String?,
    val profilePhoto: String?,
    // Employer specific fields
    val companyName: String? = null,
    val companyFunction: String? = null,
    val staffCount: Int? = null,
    val yearlyTurnover: String? = null
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class LocationDto(
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val pinCode: String?,
    val state: String?,
    val district: String?
)