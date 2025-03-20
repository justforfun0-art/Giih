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
    val name: String,
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

data class SupabaseUserData(
    val id: String,
    val phone: String,
    val firebase_uid: String? = null,
    val full_name: String? = null,
    val email: String? = null,
    val user_type: String? = null,
    val created_at: String? = null
)

data class SupabaseUser(
    val id: String,
    val phone: String,
    val firebaseUid: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val userType: UserType? = null,
    val createdAt: String? = null
)

fun SupabaseUserData.toSupabaseUser(): SupabaseUser {
    return SupabaseUser(
        id = this.id,
        phone = this.phone,
        firebaseUid = this.firebase_uid,
        fullName = this.full_name,
        email = this.email,
        userType = this.user_type?.let { UserType.valueOf(it) },
        createdAt = this.created_at
    )
}