package com.example.gigwork.data.database

import androidx.room.*
import com.example.gigwork.data.database.JobDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDraftDao {
    @Query("SELECT * FROM job_drafts ORDER BY updatedAt DESC")
    fun getAllDrafts(): Flow<List<JobDraftEntity>>

    @Query("SELECT * FROM job_drafts WHERE id = :id")
    suspend fun getDraft(id: String): JobDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: JobDraftEntity)

    @Delete
    suspend fun deleteDraft(draft: JobDraftEntity)

    @Query("DELETE FROM job_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String)

    @Query("DELETE FROM job_drafts")
    suspend fun clearAllDrafts()

    @Query("SELECT COUNT(*) FROM job_drafts")
    suspend fun getDraftCount(): Int

    @Query("SELECT * FROM job_drafts WHERE updatedAt < :timestamp")
    suspend fun getOldDrafts(timestamp: Long): List<JobDraftEntity>

    @Transaction
    @Query("""
        SELECT * FROM job_drafts 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchDrafts(query: String): Flow<List<JobDraftEntity>>

    @Query("SELECT * FROM job_drafts WHERE id IN (:ids)")
    suspend fun getDraftsByIds(ids: List<String>): List<JobDraftEntity>

    @Transaction
    suspend fun updateDraft(draft: JobDraftEntity) {
        insertDraft(draft.copy(updatedAt = System.currentTimeMillis()))
    }

    @Query("DELETE FROM job_drafts WHERE updatedAt < :timestamp")
    suspend fun deleteOldDrafts(timestamp: Long)

    @Transaction
    suspend fun cleanupOldDrafts(maxAgeInMillis: Long) {
        val cutoffTime = System.currentTimeMillis() - maxAgeInMillis
        deleteOldDrafts(cutoffTime)
    }
}