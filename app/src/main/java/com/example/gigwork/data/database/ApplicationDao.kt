// ApplicationDao.kt
package com.example.gigwork.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ApplicationDao {
    /**
     * Insert a new application
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertApplication(application: ApplicationEntity)

    /**
     * Update an existing application
     */
    @Update
    suspend fun updateApplication(application: ApplicationEntity)

    /**
     * Get application by ID
     */
    @Query("SELECT * FROM job_applications WHERE id = :applicationId")
    suspend fun getApplicationById(applicationId: String): ApplicationEntity?

    /**
     * Get application by user and job
     */
    @Query("SELECT * FROM job_applications WHERE userId = :userId AND jobId = :jobId")
    suspend fun getApplicationByUserAndJob(userId: String, jobId: String): ApplicationEntity?

    /**
     * Get all applications for a job
     */
    @Query("SELECT * FROM job_applications WHERE jobId = :jobId ORDER BY appliedAt DESC")
    suspend fun getApplicationsByJob(jobId: String): List<ApplicationEntity>

    /**
     * Get all applications by a user
     */
    @Query("SELECT * FROM job_applications WHERE userId = :userId ORDER BY appliedAt DESC")
    suspend fun getApplicationsByUser(userId: String): List<ApplicationEntity>

    /**
     * Get applications by status
     */
    @Query("SELECT * FROM job_applications WHERE status = :status ORDER BY appliedAt DESC")
    suspend fun getApplicationsByStatus(status: String): List<ApplicationEntity>

    /**
     * Count applications by status for a job
     */
    @Query("SELECT COUNT(*) FROM job_applications WHERE jobId = :jobId AND status = :status")
    suspend fun countApplicationsByStatus(jobId: String, status: String): Int

    /**
     * Count all applications for a job
     */
    @Query("SELECT COUNT(*) FROM job_applications WHERE jobId = :jobId")
    suspend fun countApplicationsForJob(jobId: String): Int

    /**
     * Delete application
     */
    @Query("DELETE FROM job_applications WHERE id = :applicationId")
    suspend fun deleteApplication(applicationId: String)

    /**
     * Update application status
     */
    @Query("UPDATE job_applications SET status = :status, updatedAt = :timestamp WHERE id = :applicationId")
    suspend fun updateApplicationStatus(applicationId: String, status: String, timestamp: Long)

    /**
     * Delete old applications
     */
    @Query("DELETE FROM job_applications WHERE appliedAt < :timestamp")
    suspend fun deleteOldApplications(timestamp: Long)

    /**
     * Get applications between dates
     */
    @Query("SELECT * FROM job_applications WHERE appliedAt BETWEEN :startTimestamp AND :endTimestamp ORDER BY appliedAt DESC")
    suspend fun getApplicationsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<ApplicationEntity>

    /**
     * Insert or update application
     */
    @Transaction
    suspend fun insertOrUpdateApplication(application: ApplicationEntity) {
        val existing = getApplicationById(application.id)
        if (existing == null) {
            insertApplication(application)
        } else {
            updateApplication(application)
        }
    }

    /**
     * Get applications with status changes in the last N days
     */
    @Query("SELECT * FROM job_applications WHERE updatedAt > :sinceTimestamp AND status != 'PENDING' ORDER BY updatedAt DESC")
    suspend fun getRecentStatusChanges(sinceTimestamp: Long): List<ApplicationEntity>

    /**
     * Get applications for multiple jobs
     */
    @Query("SELECT * FROM job_applications WHERE jobId IN (:jobIds) ORDER BY appliedAt DESC")
    suspend fun getApplicationsForJobs(jobIds: List<String>): List<ApplicationEntity>
}