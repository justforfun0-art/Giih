// domain/usecase/job/CreateJobUseCase.kt
package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.extensions.toAppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.validation.ValidationResult
import com.example.gigwork.domain.validation.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CreateJobUseCase @Inject constructor(
    private val jobRepository: JobRepository
) {
    suspend operator fun invoke(job: Job): Flow<ApiResult<Job>> = flow {
        emit(ApiResult.Loading)

        try {
            // Validate the job input using the Validators object
            when (val validationResult = Validators.validateJob(job)) {
                is ValidationResult.Success -> {
                    // If validation succeeds, create the job using the repository
                    jobRepository.createJob(job).collect { result ->
                        // Emit the result based on the API response
                        emit(result)
                    }
                }
                is ValidationResult.Error -> {
                    // If validation fails, emit a ValidationError
                    val errorMessage = validationResult.errors.joinToString(", ") { it.message }
                    val errorField = validationResult.errors.firstOrNull()?.field
                    emit(ApiResult.Error(com.example.gigwork.core.error.model.AppError.ValidationError(
                        message = errorMessage,
                        errorCode = null,
                        field = errorField
                    )))
                }
                // This else branch makes the when expression exhaustive
                else -> {
                    emit(ApiResult.Error(com.example.gigwork.core.error.model.AppError.UnexpectedError(
                        message = "Unexpected validation result",
                        cause = null
                    )))
                }
            }
        } catch (e: Exception) {
            // If an exception occurs, convert it to an AppError and emit an error result
            emit(ApiResult.Error(e.toAppError()))
        }
    }
}