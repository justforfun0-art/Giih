// util/DateUtils.kt
package com.example.gigwork.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatDate(date: String): String {
        return try {
            val parsedDate = dateFormat.parse(date)
            displayFormat.format(parsedDate!!)
        } catch (e: Exception) {
            date
        }
    }

    fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }
}