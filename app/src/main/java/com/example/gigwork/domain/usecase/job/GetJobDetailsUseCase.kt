package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Use case to retrieve detailed information about a specific job.
 */
class GetJobDetailsUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    /**
     * Retrieves detailed information about a job by its ID.
     *
     * @param jobId The ID of the job to retrieve
     * @return A Flow of ApiResult containing the Job details or an error
     */
    suspend operator fun invoke(jobId: String): Flow<ApiResult<Job>> = flow {
        // Input validation
        if (jobId.isBlank()) {
            emit(ApiResult.Error(AppError.ValidationError("Job ID cannot be empty", "jobId")))
            return@flow
        }

        emit(ApiResult.Loading)

        try {
            // Assuming getJobById returns Flow<ApiResult<Job>> instead of Flow<Result<Job>>
            jobRepository.getJobById(jobId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        emit(ApiResult.Success(result.data))
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
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to get job details",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                else -> AppError.UnexpectedError(
                    message = e.message ?: "Failed to get job details",
                    cause = e
                )
            }
            emit(ApiResult.Error(error))
        }
    }.flowOn(dispatcher)
}