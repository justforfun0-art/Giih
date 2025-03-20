package com.example.gigwork.data.repository

import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.data.database.ApplicationDao
import com.example.gigwork.data.database.ApplicationEntity
import com.example.gigwork.data.security.EncryptedPreferences
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.JobApplicationRepository
import com.example.gigwork.domain.repository.JobApplicationRepository.JobApplication
import com.example.gigwork.domain.repository.JobApplicationRepository.ApplicationStatus
import com.example.gigwork.util.Logger
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobApplicationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val applicationDao: ApplicationDao,
    private val encryptedPreferences: EncryptedPreferences,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobApplicationRepository {

    companion object {
        private const val TAG = "JobApplicationRepo"
        private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    }

    override suspend fun submitApplication(application: JobApplication): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)
        try {
            // Ensure we have a valid user ID
            val userId = application.userId ?: getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "SUBMIT_APPLICATION")))
                return@flow
            }

            // Check if user already applied for this job
            val existingApplication = withContext(ioDispatcher) {
                applicationDao.getApplicationByUserAndJob(userId, application.jobId)
            }

            if (existingApplication != null &&
                existingApplication.status != ApplicationStatus.WITHDRAWN.name) {
                logger.w(
                    tag = TAG,
                    message = "User already applied for this job",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "job_id" to application.jobId,
                        "current_status" to existingApplication.status
                    )
                )
                emit(ApiResult.Error(ExceptionMapper.map(
                    IllegalStateException("You have already applied for this job"),
                    "SUBMIT_APPLICATION_DUPLICATE"
                )))
                return@flow
            }

            // Generate application ID if not present in existing application
            val applicationId = existingApplication?.id ?: UUID.randomUUID().toString()

            // Submit to remote database
            withContext(ioDispatcher) {
                val payload = mapOf(
                    "id" to applicationId,
                    "user_id" to userId,
                    "job_id" to application.jobId,
                    "status" to ApplicationStatus.PENDING.name,
                    "attachments" to application.attachments,
                    "applied_at" to application.appliedAt,
                    "updated_at" to System.currentTimeMillis()
                )

                if (existingApplication == null) {
                    // Create new application
                    supabaseClient.client.postgrest["job_applications"]
                        .insert(payload)
                } else {
                    // Update withdrawn application to pending
                    supabaseClient.client.postgrest["job_applications"]
                        .update(payload) {
                            filter { eq("id", applicationId)}
                        }
                }
            }

            // Update local database
            val entity = ApplicationEntity(
                id = applicationId,
                userId = userId,
                jobId = application.jobId,
                status = ApplicationStatus.PENDING.name,
                attachments = application.attachments.joinToString(","),
                appliedAt = application.appliedAt,
                updatedAt = System.currentTimeMillis()
            )

            if (existingApplication == null) {
                applicationDao.insertApplication(entity)
            } else {
                applicationDao.updateApplication(entity)
            }

            logger.i(
                tag = TAG,
                message = "Successfully submitted job application",
                additionalData = mapOf(
                    "application_id" to applicationId,
                    "user_id" to userId,
                    "job_id" to application.jobId
                )
            )

            emit(ApiResult.Success(applicationId))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error submitting job application",
                throwable = e,
                additionalData = mapOf(
                    "job_id" to application.jobId,
                    "user_id" to application.userId
                )
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "SUBMIT_APPLICATION")))
        }
    }

    override suspend fun getApplicationStatus(jobId: String): Flow<ApiResult<ApplicationStatus>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Success(ApplicationStatus.NOT_APPLIED))
                return@flow
            }

            // Check local database first
            val cachedApplication = withContext(ioDispatcher) {
                applicationDao.getApplicationByUserAndJob(userId, jobId)
            }

            if (cachedApplication != null) {
                val status = try {
                    ApplicationStatus.valueOf(cachedApplication.status)
                } catch (e: IllegalArgumentException) {
                    ApplicationStatus.NOT_APPLIED
                }

                emit(ApiResult.Success(status))
            }

            // Check remote database
            withContext(ioDispatcher) {
                try {
                    val application = supabaseClient.client.postgrest["job_applications"]
                        .select {
                            filter { eq("user_id", userId)}
                            filter { eq("job_id", jobId)}
                        }
                        .decodeSingleOrNull<Map<String, Any?>>()

                    if (application != null) {
                        val statusStr = application["status"] as? String
                        val status = if (statusStr != null) {
                            try {
                                ApplicationStatus.valueOf(statusStr)
                            } catch (e: IllegalArgumentException) {
                                ApplicationStatus.NOT_APPLIED
                            }
                        } else {
                            ApplicationStatus.NOT_APPLIED
                        }

                        // Update local database
                        val entity = ApplicationEntity(
                            id = application["id"] as String,
                            userId = userId,
                            jobId = jobId,
                            status = status.name,
                            attachments = (application["attachments"] as? List<String>)?.joinToString(",") ?: "",
                            appliedAt = (application["applied_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (application["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )

                        if (cachedApplication == null) {
                            applicationDao.insertApplication(entity)
                        } else {
                            applicationDao.updateApplication(entity)
                        }

                        // Only emit if different from cached status
                        if (cachedApplication == null || cachedApplication.status != status.name) {
                            emit(ApiResult.Success(status))
                        }
                    } else if (cachedApplication == null) {
                        // Only emit if no cached status
                        emit(ApiResult.Success(ApplicationStatus.NOT_APPLIED))
                    }
                } catch (e: Exception) {
                    // If remote check fails but we have cached status, don't emit error
                    if (cachedApplication == null) {
                        logger.w(
                            tag = TAG,
                            message = "Error checking application status from network",
                            throwable = e,
                            additionalData = mapOf(
                                "user_id" to userId,
                                "job_id" to jobId
                            )
                        )
                        emit(ApiResult.Success(ApplicationStatus.NOT_APPLIED))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting application status",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_APPLICATION_STATUS")))
        }
    }

    override suspend fun withdrawApplication(jobId: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "WITHDRAW_APPLICATION")))
                return@flow
            }

            // Check if application exists
            val cachedApplication = withContext(ioDispatcher) {
                applicationDao.getApplicationByUserAndJob(userId, jobId)
            }

            if (cachedApplication == null) {
                logger.w(
                    tag = TAG,
                    message = "No application found to withdraw",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "job_id" to jobId
                    )
                )
                emit(ApiResult.Error(ExceptionMapper.map(
                    NoSuchElementException("No application found for this job"),
                    "WITHDRAW_APPLICATION_NOT_FOUND"
                )))
                return@flow
            }

            // Update remote database
            withContext(ioDispatcher) {
                supabaseClient.client.postgrest["job_applications"]
                    .update(mapOf("status" to ApplicationStatus.WITHDRAWN.name, "updated_at" to System.currentTimeMillis())) {
                        filter { eq("user_id", userId)}
                        filter { eq("job_id", jobId)}
                    }
            }

            // Update local database
            val updatedEntity = cachedApplication.copy(
                status = ApplicationStatus.WITHDRAWN.name,
                updatedAt = System.currentTimeMillis()
            )
            applicationDao.updateApplication(updatedEntity)

            logger.i(
                tag = TAG,
                message = "Successfully withdrew job application",
                additionalData = mapOf(
                    "application_id" to cachedApplication.id,
                    "user_id" to userId,
                    "job_id" to jobId
                )
            )

            emit(ApiResult.Success(true))
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error withdrawing job application",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "WITHDRAW_APPLICATION")))
        }
    }

    override suspend fun getJobApplications(jobId: String): Flow<ApiResult<List<JobApplication>>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get from local cache first
            val cachedApplications = withContext(ioDispatcher) {
                applicationDao.getApplicationsByJob(jobId)
            }

            if (cachedApplications.isNotEmpty()) {
                val domainApplications = cachedApplications.map { mapEntityToDomain(it) }
                logger.d(
                    tag = TAG,
                    message = "Returning cached job applications",
                    additionalData = mapOf(
                        "job_id" to jobId,
                        "count" to domainApplications.size
                    )
                )
                emit(ApiResult.Success(domainApplications))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    val applications = supabaseClient.client.postgrest["job_applications"]
                        .select {
                            filter { eq("job_id", jobId)}
                        }
                        .decodeList<Map<String, Any?>>()

                    val domainApplications = applications.map { mapResponseToDomain(it) }

                    // Update local cache
                    domainApplications.forEach {
                        val entity = mapDomainToEntity(it)
                        applicationDao.insertOrUpdateApplication(entity)
                    }

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched job applications from network",
                        additionalData = mapOf(
                            "job_id" to jobId,
                            "count" to domainApplications.size
                        )
                    )

                    // Only emit if different from cache
                    if (cachedApplications.size != domainApplications.size) {
                        emit(ApiResult.Success(domainApplications))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedApplications.isEmpty()) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching job applications from network",
                            throwable = e,
                            additionalData = mapOf("job_id" to jobId)
                        )
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOB_APPLICATIONS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting job applications",
                throwable = e,
                additionalData = mapOf("job_id" to jobId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_JOB_APPLICATIONS")))
        }
    }

    override suspend fun getUserApplications(): Flow<ApiResult<List<JobApplication>>> = flow {
        emit(ApiResult.Loading)
        try {
            val userId = getCurrentUserId()
            if (userId.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(IllegalStateException("User not logged in"), "GET_USER_APPLICATIONS")))
                return@flow
            }

            // Get from local cache first
            val cachedApplications = withContext(ioDispatcher) {
                applicationDao.getApplicationsByUser(userId)
            }

            if (cachedApplications.isNotEmpty()) {
                val domainApplications = cachedApplications.map { mapEntityToDomain(it) }
                logger.d(
                    tag = TAG,
                    message = "Returning cached user applications",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "count" to domainApplications.size
                    )
                )
                emit(ApiResult.Success(domainApplications))
            }

            // Fetch from remote
            withContext(ioDispatcher) {
                try {
                    val applications = supabaseClient.client.postgrest["job_applications"]
                        .select {
                            filter { eq("user_id", userId)}
                        }
                        .decodeList<Map<String, Any?>>()

                    val domainApplications = applications.map { mapResponseToDomain(it) }

                    // Update local cache
                    domainApplications.forEach {
                        val entity = mapDomainToEntity(it)
                        applicationDao.insertOrUpdateApplication(entity)
                    }

                    logger.i(
                        tag = TAG,
                        message = "Successfully fetched user applications from network",
                        additionalData = mapOf(
                            "user_id" to userId,
                            "count" to domainApplications.size
                        )
                    )

                    // Only emit if different from cache
                    if (cachedApplications.size != domainApplications.size) {
                        emit(ApiResult.Success(domainApplications))
                    }
                } catch (e: Exception) {
                    // If remote fetch fails but we have cached data, don't emit error
                    if (cachedApplications.isEmpty()) {
                        logger.w(
                            tag = TAG,
                            message = "Error fetching user applications from network",
                            throwable = e,
                            additionalData = mapOf("user_id" to userId)
                        )
                        emit(ApiResult.Error(ExceptionMapper.map(e, "GET_USER_APPLICATIONS_NETWORK")))
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting user applications",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_USER_APPLICATIONS")))
        }
    }

    private fun getCurrentUserId(): String {
        return encryptedPreferences.getUserId()
    }

    private fun mapEntityToDomain(entity: ApplicationEntity): JobApplication {
        val attachments = if (entity.attachments.isBlank()) {
            emptyList()
        } else {
            entity.attachments.split(",")
        }

        val status = try {
            ApplicationStatus.valueOf(entity.status)
        } catch (e: IllegalArgumentException) {
            ApplicationStatus.PENDING
        }

        return JobApplication(
            jobId = entity.jobId,
            userId = entity.userId,
            attachments = attachments,
            appliedAt = entity.appliedAt,
            status = status
        )
    }

    private fun mapResponseToDomain(response: Map<String, Any?>): JobApplication {
        val attachments = (response["attachments"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        val statusStr = response["status"] as? String
        val status = if (statusStr != null) {
            try {
                ApplicationStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                ApplicationStatus.PENDING
            }
        } else {
            ApplicationStatus.PENDING
        }

        return JobApplication(
            jobId = response["job_id"] as String,
            userId = response["user_id"] as String,
            attachments = attachments,
            appliedAt = (response["applied_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            status = status
        )
    }

    private fun mapDomainToEntity(application: JobApplication): ApplicationEntity {
        return ApplicationEntity(
            id = UUID.randomUUID().toString(), // This will be overwritten if entity exists
            userId = application.userId ?: "",
            jobId = application.jobId,
            status = application.status.name,
            attachments = application.attachments.joinToString(","),
            appliedAt = application.appliedAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}