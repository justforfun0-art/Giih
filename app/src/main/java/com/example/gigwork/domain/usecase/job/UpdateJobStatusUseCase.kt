package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateJobStatusUseCase @Inject constructor(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, status: String): Flow<ApiResult<Job>> = flow {
        try {
            if (status !in listOf("OPEN", "CLOSED", "PENDING", "DELETED")) {
                throw IllegalArgumentException("Invalid job status")
            }

            repository.updateJobStatus(jobId, status).collect { result ->
                when (result) {
                    is ApiResult.Success -> emit(ApiResult.Success(result.data))
                    is ApiResult.Error -> emit(ApiResult.Error(result.error))
                    is ApiResult.Loading -> emit(ApiResult.Loading)
                }
            }
        } catch (e: Exception) {
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Validation error occurred")
                else -> AppError.UnexpectedError(e.message ?: "An unexpected error occurred", e)
            }
            emit(ApiResult.Error(appError))
        }
    }
}