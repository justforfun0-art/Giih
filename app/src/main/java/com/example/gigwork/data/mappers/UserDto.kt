// data/models/UserDto.kt
package com.example.gigwork.data.models

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val type: String,
    val profile: UserProfileDto? = null
)

@Serializable
data class UserProfileDto(
    val id: String,
    val userId: String,
    val name: String,
    val dateOfBirth: String?,
    val gender: String?,
    val currentLocation: LocationDto,
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