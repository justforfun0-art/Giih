// util/ValidationUtils.kt
package com.example.gigwork.util

object ValidationUtils {
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.matches(Regex("^[0-9]{10}$"))
    }

    fun isValidPassword(password: String): Boolean {
        return password.length >= 8
    }
}
