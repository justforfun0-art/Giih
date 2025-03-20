package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.JobApplicationRepository
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case that handles applying for a job and checking application status.
 */
class ApplyForJobUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    private val jobApplicationRepository: JobApplicationRepository,
    private val userRepository: UserRepository,
    private val logger: Logger,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    companion object {
        private const val TAG = "ApplyForJobUseCase"
    }

    /**
     * Apply for a job with the given job ID.
     *
     * @param jobId The ID of the job to apply for
     * @return A Flow of ApiResult indicating success, loading state, or error
     */
    suspend operator fun invoke(jobId: String): Flow<ApiResult<Unit>> = flow {
        // Input validation
        if (jobId.isBlank()) {
            emit(ApiResult.Error(AppError.ValidationError("Job ID cannot be empty", "jobId")))
            return@flow
        }

        emit(ApiResult.Loading)

        try {
            // Check if the job exists and is open for applications
            val jobResult = jobRepository.getJobById(jobId).map { result ->
                result.getOrNull()
            }.flowOn(dispatcher)

            var job: com.example.gigwork.domain.models.Job? = null
            jobResult.collect {
                job = it
            }

            if (job == null) {
                emit(ApiResult.Error(AppError.BusinessError("Job not found or unavailable")))
                return@flow
            }

            if (job?.status?.uppercase() != "OPEN") {
                emit(ApiResult.Error(AppError.BusinessError("This job is not open for applications")))
                return@flow
            }

            // Check if user has already applied
            // Check if user has already applied
            var alreadyApplied = false
            jobApplicationRepository.getApplicationStatus(jobId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        if (result.data != JobApplicationRepository.ApplicationStatus.NOT_APPLIED) {
                            alreadyApplied = true
                        }
                    }
                    is ApiResult.Error -> {
                        logger.e(
                            tag = TAG,
                            message = "Error checking application status",
                            throwable = result.error
                        )
                    }
                    is ApiResult.Loading -> {
                        // We can ignore loading state here
                    }
                }
            }

            if (alreadyApplied) {
                emit(ApiResult.Error(AppError.BusinessError("You have already applied for this job")))
                return@flow
            }

            // Create the application
            // Create the application object with the required fields
            val application = JobApplicationRepository.JobApplication(
                jobId = jobId,
                appliedAt = System.currentTimeMillis()
            )

// Submit the application using the correct method name and parameter
            jobApplicationRepository.submitApplication(application).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        logger.i(
                            tag = TAG,
                            message = "Successfully applied for job",
                            additionalData = mapOf("jobId" to jobId, "applicationId" to result.data)
                        )
                        emit(ApiResult.Success(Unit))
                    }
                    is ApiResult.Error -> {
                        emit(ApiResult.Error(result.error))
                    }
                    is ApiResult.Loading -> {
                        emit(ApiResult.Loading)
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error applying for job",
                throwable = e
            )
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to apply for job",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        }
    }.flowOn(dispatcher)

    /**
     * Check if the current user has already applied for a specific job.
     *
     * @param jobId The ID of the job to check
     * @return A Flow of ApiResult containing a boolean indicating if the user has applied
     */
    /**
     * Check if the current user has already applied for a specific job.
     *
     * @param jobId The ID of the job to check
     * @return A Flow of ApiResult containing a boolean indicating if the user has applied
     */
    suspend fun hasApplied(jobId: String): Flow<ApiResult<Boolean>> = flow {
        // Input validation
        if (jobId.isBlank()) {
            emit(ApiResult.Error(AppError.ValidationError("Job ID cannot be empty", "jobId")))
            return@flow
        }

        emit(ApiResult.Loading)

        try {
            // Check application status using getApplicationStatus
            jobApplicationRepository.getApplicationStatus(jobId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val hasApplied = result.data != JobApplicationRepository.ApplicationStatus.NOT_APPLIED
                        emit(ApiResult.Success(hasApplied))
                    }
                    is ApiResult.Error -> {
                        emit(ApiResult.Error(result.error))
                    }
                    is ApiResult.Loading -> {
                        emit(ApiResult.Loading)
                    }
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error checking application status",
                throwable = e
            )
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to check application status",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        }
    }.flowOn(dispatcher)
}