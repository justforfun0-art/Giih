// domain/models/UserRole.kt
package com.example.gigwork.domain.models

enum class UserRole {
    EMPLOYER,
    EMPLOYEE,
    GUEST;

    companion object {
        fun fromString(role: String?): UserRole {
            return when (role?.uppercase()) {
                "EMPLOYER" -> EMPLOYER
                "EMPLOYEE" -> EMPLOYEE
                else -> GUEST
            }
        }

        fun UserType.toUserRole(): UserRole {
            return when (this) {
                UserType.EMPLOYER -> UserRole.EMPLOYER
                UserType.EMPLOYEE -> UserRole.EMPLOYEE
            }
        }

        /**
         * Extension function to convert UserRole to UserType
         * Returns null for GUEST role since it doesn't map to a UserType
         */
        fun UserRole.toUserType(): UserType? {
            return when (this) {
                UserRole.EMPLOYER -> UserType.EMPLOYER
                UserRole.EMPLOYEE -> UserType.EMPLOYEE
                UserRole.GUEST -> null
            }
        }
    }
}