// FavoriteLocationEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_locations",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["state", "district"])
    ]
)
data class FavoriteLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val state: String,
    val district: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)