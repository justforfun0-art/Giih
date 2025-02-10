// data/database/migrations/JobDraftMigrations.kt
package com.example.gigwork.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS job_drafts (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                salary REAL NOT NULL,
                salaryUnit TEXT NOT NULL,
                workDuration INTEGER NOT NULL,
                workDurationUnit TEXT NOT NULL,
                location TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                lastModified INTEGER NOT NULL
            )
        """)
    }
}