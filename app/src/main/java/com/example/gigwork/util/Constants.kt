// util/Constants.kt
package com.example.gigwork.util

object Constants {
    const val BASE_URL = "https://api.postalpincode.in/"
    const val TIMEOUT_SECONDS = 30L

    object PreferenceKeys {
        const val USER_TOKEN = "user_token"
        const val USER_ID = "user_id"
        const val USER_TYPE = "user_type"
    }

    object UserType {
        const val EMPLOYEE = "EMPLOYEE"
        const val EMPLOYER = "EMPLOYER"
    }
}