// BookmarkEntity.kt
package com.example.gigwork.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    indices = [
        Index(value = ["userId", "jobId"], unique = true),
        Index(value = ["jobId"])
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val jobId: String,
    val timestamp: Long
)