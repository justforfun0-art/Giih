// data/database/AppDatabase.kt
package com.example.gigwork.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gigwork.data.database.dao.JobDao
import com.example.gigwork.data.database.JobEntity

@Database(
    entities = [JobEntity::class, JobDraftEntity::class],
    version = 1, exportSchema = false
)
@TypeConverters(LocationConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun jobDraftDao(): JobDraftDao
}