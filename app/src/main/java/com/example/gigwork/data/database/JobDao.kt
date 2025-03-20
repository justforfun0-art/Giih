// data/database/dao/JobDao.kt
package com.example.gigwork.data.database.dao

import androidx.room.*
import com.example.gigwork.data.database.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("""
        SELECT * FROM jobs 
        WHERE (:state IS NULL OR locationState = :state)
        AND (:district IS NULL OR locationDistrict = :district)
        AND (:minSalary IS NULL OR salary >= :minSalary)
        AND (:maxSalary IS NULL OR salary <= :maxSalary)
        ORDER BY createdAt DESC
        LIMIT :pageSize OFFSET :offset
    """)
    fun getPagedJobs(
        state: String?,
        district: String?,
        minSalary: Double?,
        maxSalary: Double?,
        pageSize: Int,
        offset: Int
    ): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE employerId = :employerId 
        ORDER BY createdAt DESC
    """)
    fun getEmployerJobs(employerId: String): Flow<List<JobEntity>>

    @Query("""
        SELECT * FROM jobs 
        WHERE title LIKE '%' || :query || '%' 
        OR description LIKE '%' || :query || '%' 
        ORDER BY createdAt DESC
    """)
    fun searchJobs(query: String): Flow<List<JobEntity>>

    @Query("""
    SELECT * FROM jobs 
    WHERE (
        6371 * acos(
            cos(radians(:latitude)) * cos(radians(locationLatitude)) * 
            cos(radians(locationLongitude) - radians(:longitude)) + 
            sin(radians(:latitude)) * sin(radians(locationLatitude))
        )
    ) <= :radiusInKm
    ORDER BY createdAt DESC
""")
    fun getNearbyJobs(
        latitude: Double,
        longitude: Double,
        radiusInKm: Double
    ): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :jobId")
    suspend fun getJobById(jobId: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobEntity)

    @Query("DELETE FROM jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("DELETE FROM jobs WHERE createdAt < :timestamp")
    suspend fun deleteOldJobs(timestamp: Long)

    @Query("DELETE FROM jobs")
    suspend fun clearAllJobs()

    // In JobDao.kt
    @Query("SELECT * FROM jobs WHERE id IN (:jobIds)")
    suspend fun getJobsByIds(jobIds: List<String>): List<JobEntity>

    @Transaction
    @Query("""
        SELECT * FROM jobs
        WHERE status = 'OPEN'
        AND (
            (:categories IS NULL) OR
            (category IN (:categories))
        )
        AND (
            (:minSalary IS NULL) OR
            (salary >= :minSalary)
        )
        AND (
            (:maxSalary IS NULL) OR
            (salary <= :maxSalary)
        )
        ORDER BY createdAt DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getFilteredJobs(
        categories: List<String>? = null,
        minSalary: Double? = null,
        maxSalary: Double? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Flow<List<JobEntity>>

    @Query("SELECT COUNT(*) FROM jobs")
    suspend fun getJobCount(): Int

    @Query("""
        SELECT COUNT(*) FROM jobs
        WHERE locationState = :state
        AND locationDistrict = :district
    """)
    suspend fun getJobCountByLocation(state: String, district: String): Int

    @Transaction
    suspend fun updateJobWithStatus(jobId: String, status: String) {
        getJobById(jobId)?.let { job ->
            insertJob(job.copy(status = status))
        }
    }

    @Query("""
        SELECT * FROM jobs
        WHERE status = :status
        ORDER BY createdAt DESC
    """)
    fun getJobsByStatus(status: String): Flow<List<JobEntity>>

    @Transaction
    suspend fun insertOrUpdateJob(job: JobEntity) {
        val existingJob = getJobById(job.id)
        if (existingJob != null) {
            // Preserve creation timestamp if job exists
            insertJob(job.copy(createdAt = existingJob.createdAt))
        } else {
            insertJob(job)
        }
    }

    @Query("""
    SELECT * FROM jobs 
    WHERE employerId = :employerId
    AND (:query IS NULL OR title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
    AND (:status IS NULL OR status = :status)
    AND (:minSalary IS NULL OR salary >= :minSalary)
    AND (:maxSalary IS NULL OR salary <= :maxSalary)
    AND (:state IS NULL OR locationstate = :state)
    AND (:district IS NULL OR locationdistrict = :district)
    ORDER BY createdAt DESC
    LIMIT :limit OFFSET :offset
""")


    fun getFilteredEmployerJobs(
        employerId: String,
        query: String?,
        status: String?,
        minSalary: Double?,
        maxSalary: Double?,
        state: String?,
        district: String?,
        limit: Int,
        offset: Int
    ): Flow<List<JobEntity>>
}