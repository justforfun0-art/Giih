// LocationEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_locations",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["timestamp"])
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val state: String,
    val district: String,
    val address: String,
    val pinCode: String,
    val timestamp: Long
)