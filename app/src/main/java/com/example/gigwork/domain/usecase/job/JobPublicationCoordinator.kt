package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.JobDraftRepository
import com.example.gigwork.presentation.states.JobDraft
import com.example.gigwork.util.Logger
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.example.gigwork.domain.models.Location


class JobPublicationCoordinator @Inject constructor(
    private val jobRepository: JobRepository,
    private val draftRepository: JobDraftRepository,
    private val logger: Logger
) {
    companion object {
        private const val TAG = "JobPublicationCoordinator"
    }

    suspend fun publishDraft(draftId: String): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)
        try {
            // Get and validate the draft
            val draft = draftRepository.getDraft(draftId)
                .first()
                .let { draftResult ->
                    when (draftResult) {
                        is ApiResult.Success -> draftResult.data
                        is ApiResult.Error -> {
                            emit(draftResult)
                            return@flow
                        }
                        ApiResult.Loading -> {
                            return@flow
                        }
                    }
                }

            if (draft == null) {
                emit(ApiResult.Error(AppError.ValidationError(
                    message = "Draft not found or invalid",
                    field = "draftId",
                    value = draftId
                )))
                return@flow
            }

            // Convert draft to job
            val draftJob = Job(
                id = "",
                employerId = "",
                title = draft.title,
                description = draft.description,
                salary = draft.salary,
                salaryUnit = draft.salaryUnit,
                workDuration = draft.workDuration,
                workDurationUnit = draft.workDurationUnit,
                location = draft.location,
                status = "ACTIVE",
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString(),
                lastModified = System.currentTimeMillis().toString(),
                company = "" // Added missing company field
            )

            // Create the job
            jobRepository.createJob(draftJob)
                .collect { createResult ->
                    when (createResult) {
                        is ApiResult.Success -> {
                            val jobResult = createResult.data
                            // Try to delete the draft after successful publication
                            try {
                                draftRepository.deleteDraft(draftId)
                                    .collect { deleteResult ->
                                        if (deleteResult is ApiResult.Error) {
                                            logger.w(
                                                tag = TAG,
                                                message = "Failed to delete draft after successful publication",
                                                throwable = deleteResult.error.cause,
                                                additionalData = mapOf(
                                                    "draftId" to draftId,
                                                    "jobId" to jobResult.id,
                                                    "error" to deleteResult.error.message
                                                )
                                            )
                                        }
                                    }
                            } catch (e: Exception) {
                                logger.w(
                                    tag = TAG,
                                    message = "Exception while deleting draft after successful publication",
                                    throwable = e,
                                    additionalData = mapOf(
                                        "draftId" to draftId,
                                        "jobId" to jobResult.id
                                    )
                                )
                            }

                            emit(ApiResult.Success(jobResult))
                            logger.d(
                                tag = TAG,
                                message = "Draft published successfully",
                                additionalData = mapOf(
                                    "draftId" to draftId,
                                    "jobId" to jobResult.id
                                )
                            )
                        }
                        is ApiResult.Error -> {
                            emit(ApiResult.Error(AppError.UnexpectedError(
                                message = "Failed to create job from draft",
                                cause = createResult.error.cause,
                                stackTrace = createResult.error.cause?.stackTraceToString()
                            )))
                        }
                        ApiResult.Loading -> {
                            // Do nothing, already emitting Loading
                        }
                    }
                }

        } catch (e: Exception) {
            val error = AppError.UnexpectedError(
                message = "Unexpected error publishing draft: ${e.message}",
                cause = e,
                stackTrace = e.stackTraceToString()
            )
            emit(ApiResult.Error(error))
            logger.e(
                tag = TAG,
                message = "Unexpected error in publish operation",
                throwable = e,
                additionalData = mapOf(
                    "draftId" to draftId,
                    "errorMessage" to e.message
                )
            )
        }
    }

    suspend fun createDraftFromJob(jobId: String): Flow<ApiResult<JobDraft>> = flow {
        emit(ApiResult.Loading)
        try {
            // Use a local final variable to store the draft
            val draftResult = jobRepository.getJobById(jobId)
                .first()

            // Process the job result outside of the collect scope
            when (draftResult) {
                is ApiResult.Success -> {
                    val job = draftResult.data
                    // Create a draft from the job
                    val draft = JobDraft(
                        id = "${job.id}_draft",
                        title = job.title,
                        description = job.description,
                        salary = job.salary,
                        salaryUnit = job.salaryUnit,
                        workDuration = job.workDuration,
                        workDurationUnit = job.workDurationUnit,
                        location = job.location,
                        lastModified = System.currentTimeMillis()
                    )

                    // Now save the draft
                    draftRepository.saveDraft(draft)
                        .collect { saveResult ->
                            when (saveResult) {
                                is ApiResult.Success -> {
                                    emit(ApiResult.Success(draft))
                                    logger.d(
                                        tag = TAG,
                                        message = "Draft created from job",
                                        additionalData = mapOf(
                                            "jobId" to jobId,
                                            "draftId" to draft.id
                                        )
                                    )
                                }
                                is ApiResult.Error -> {
                                    emit(saveResult)
                                    logger.e(
                                        tag = TAG,
                                        message = "Failed to save draft",
                                        throwable = saveResult.error.cause,
                                        additionalData = mapOf(
                                            "jobId" to jobId,
                                            "errorMessage" to saveResult.error.message
                                        )
                                    )
                                }
                                ApiResult.Loading -> Unit
                            }
                        }
                }
                is ApiResult.Error -> {
                    emit(ApiResult.Error(AppError.UnexpectedError(
                        message = "Failed to fetch job",
                        cause = draftResult.error.cause,
                        stackTrace = draftResult.error.cause?.stackTraceToString()
                    )))
                }
                ApiResult.Loading -> {
                    // Already emitting Loading above
                }
            }
        } catch (e: Exception) {
            val error = AppError.UnexpectedError(
                message = "Unexpected error creating draft from job",
                cause = e,
                stackTrace = e.stackTraceToString()
            )
            emit(ApiResult.Error(error))
            logger.e(
                tag = TAG,
                message = "Unexpected error creating draft",
                throwable = e,
                additionalData = mapOf(
                    "jobId" to jobId
                )
            )
        }
    }

    private fun createDefaultLocation() = Location(
        state = "",
        district = "",
        latitude = 0.0,
        longitude = 0.0,
        address = null,
        pinCode = null
    )

    suspend fun updateJobFromDraft(jobId: String, draftId: String): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)
        try {
            // First, retrieve and validate the draft
            val draftFlow = draftRepository.getDraft(draftId)
                .first()

            val draft = when (draftFlow) {
                is ApiResult.Success -> draftFlow.data
                is ApiResult.Error -> {
                    emit(draftFlow)
                    return@flow
                }
                ApiResult.Loading -> {
                    return@flow
                }
            }

            if (draft == null) {
                emit(ApiResult.Error(AppError.ValidationError(
                    message = "Draft not found",
                    field = "draftId",
                    value = draftId
                )))
                return@flow
            }

            // Convert draft to job
            val updatedJob = Job(
                id = jobId,
                employerId = "",
                title = draft.title,
                description = draft.description,
                salary = draft.salary,
                salaryUnit = draft.salaryUnit,
                workDuration = draft.workDuration,
                workDurationUnit = draft.workDurationUnit,
                location = draft.location,
                status = "ACTIVE",
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString(),
                lastModified = System.currentTimeMillis().toString(),
                company = "" // Added missing company field
            )

            // Update the job using the repository
            jobRepository.updateJob(jobId, updatedJob)
                .collect { updateResult ->
                    when (updateResult) {
                        is ApiResult.Success -> {
                            val updatedJobResult = updateResult.data
                            // Attempt to delete the draft after successful update
                            try {
                                draftRepository.deleteDraft(draftId)
                                    .collect { deleteResult ->
                                        if (deleteResult is ApiResult.Error) {
                                            logger.w(
                                                tag = TAG,
                                                message = "Failed to delete draft after successful update",
                                                throwable = deleteResult.error.cause,
                                                additionalData = mapOf(
                                                    "draftId" to draftId,
                                                    "jobId" to jobId,
                                                    "error" to deleteResult.error.message
                                                )
                                            )
                                        }
                                    }
                            } catch (e: Exception) {
                                logger.w(
                                    tag = TAG,
                                    message = "Exception while deleting draft",
                                    throwable = e,
                                    additionalData = mapOf(
                                        "draftId" to draftId,
                                        "jobId" to jobId
                                    )
                                )
                            }

                            emit(ApiResult.Success(updatedJobResult))
                            logger.d(
                                tag = TAG,
                                message = "Job updated from draft successfully",
                                additionalData = mapOf(
                                    "jobId" to jobId,
                                    "draftId" to draftId
                                )
                            )
                        }
                        is ApiResult.Error -> {
                            emit(ApiResult.Error(AppError.UnexpectedError(
                                message = "Failed to update job",
                                cause = updateResult.error.cause,
                                stackTrace = updateResult.error.cause?.stackTraceToString()
                            )))
                            logger.e(
                                tag = TAG,
                                message = "Failed to update job",
                                throwable = updateResult.error.cause,
                                additionalData = mapOf(
                                    "jobId" to jobId,
                                    "draftId" to draftId,
                                    "errorMessage" to updateResult.error.message
                                )
                            )
                        }
                        ApiResult.Loading -> {
                            // Do nothing, already emitting Loading
                        }
                    }
                }

        } catch (e: Exception) {
            val error = AppError.UnexpectedError(
                message = "Unexpected error updating job from draft",
                cause = e,
                stackTrace = e.stackTraceToString()
            )
            emit(ApiResult.Error(error))
            logger.e(
                tag = TAG,
                message = "Unexpected error in update job operation",
                throwable = e,
                additionalData = mapOf(
                    "jobId" to jobId,
                    "draftId" to draftId
                )
            )
        }
    }

    // Extension functions
    private fun JobDraft.toJob() = Job(
        id = "",  // Empty as new ID will be assigned by backend
        employerId = "",  // This should be set to the actual employer ID if available
        title = title,
        description = description,
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        location = location,
        status = "ACTIVE",
        createdAt = System.currentTimeMillis().toString(),
        updatedAt = System.currentTimeMillis().toString(),
        lastModified = System.currentTimeMillis().toString(),
        company = "" // Added missing company field
    )

    private fun Job.toDraft() = JobDraft(
        id = "${this.id}_draft",
        title = title,
        description = description,
        salary = salary,
        salaryUnit = salaryUnit,
        workDuration = workDuration,
        workDurationUnit = workDurationUnit,
        location = location,
        lastModified = System.currentTimeMillis()
    )
}