// domain/usecase/job/CreateJobUseCase.kt
package com.example.gigwork.domain.usecase.job

import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.validation.ValidationResult
import com.example.gigwork.domain.validation.Validators
import com.example.gigwork.util.AppError
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CreateJobUseCase @Inject constructor(
    private val jobRepository: JobRepository
) {
    suspend operator fun invoke(job: Job): Flow<Result<Job>> = flow {
        emit(Result.Loading)

        try {
            // Validate job input
            when (val validationResult = Validators.validateJob(job)) {
                is ValidationResult.Success -> {
                    jobRepository.createJob(job).collect { result ->
                        when (result) {
                            is Result.Success -> emit(Result.Success(result.data))
                            is Result.Error -> emit(Result.Error(result.exception))
                            is Result.Loading -> emit(Result.Loading)
                        }
                    }
                }
                is ValidationResult.Error -> {
                    emit(Result.Error(AppError.ValidationError(
                        validationResult.errors.joinToString(", ") { it.message },
                        validationResult.errors.firstOrNull()?.field
                    )))
                }
            }
        } catch (e: Exception) {
            emit(Result.Error(e.toAppError()))
        }
    }
}
