// domain/models/User.kt
package com.example.gigwork.domain.models

data class User(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val type: UserType,
    val profile: UserProfile? = null
)

enum class UserType {
    EMPLOYEE,
    EMPLOYER
}

data class UserProfile(
    val id: String,
    val userId: String,
    val photo: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val currentLocation: Location?,
    val preferredLocation: Location?,
    val qualification: String?,
    val computerKnowledge: Boolean?,
    val aadharNumber: String?,
    // For employer
    val companyName: String?,
    val companyFunction: String?,
    val staffCount: Int?,
    val yearlyTurnover: String?
)