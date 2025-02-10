package com.example.gigwork.domain.usecase.job

import android.util.Patterns
import com.example.gigwork.core.error.AppError
import com.example.gigwork.core.result.Result
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.domain.usecase.base.FlowUseCase
import com.example.gigwork.domain.usecase.base.SyncUseCase
import com.example.gigwork.domain.usecase.base.FlowNoParamsUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetJobsUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<GetJobsUseCase.Params, List<Job>>(dispatcher) {

    data class Params(
        val page: Int = 1,
        val pageSize: Int = 20,
        val state: String? = null,
        val district: String? = null,
        val radius: Double? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val minSalary: Double? = null,
        val maxSalary: Double? = null,
        val forceRefresh: Boolean = false
    ) {
        companion object {
            fun default() = Params()

            fun location(state: String, district: String) = Params(
                state = state,
                district = district
            )

            fun nearby(latitude: Double, longitude: Double, radius: Double) = Params(
                latitude = latitude,
                longitude = longitude,
                radius = radius
            )

            fun salary(min: Double? = null, max: Double? = null) = Params(
                minSalary = min,
                maxSalary = max
            )
        }

        fun validate(): Result<Params> {
            return when {
                page < 1 -> Result.Error(
                    AppError.ValidationError(
                        message = "Page number must be greater than 0",
                        field = "page"
                    )
                )
                pageSize < 1 -> Result.Error(
                    AppError.ValidationError(
                        message = "Page size must be greater than 0",
                        field = "pageSize"
                    )
                )
                radius != null && (latitude == null || longitude == null) -> Result.Error(
                    AppError.ValidationError(
                        message = "Latitude and longitude are required when radius is specified",
                        field = "location"
                    )
                )
                radius != null && radius <= 0 -> Result.Error(
                    AppError.ValidationError(
                        message = "Radius must be greater than 0",
                        field = "radius"
                    )
                )
                minSalary != null && minSalary < 0 -> Result.Error(
                    AppError.ValidationError(
                        message = "Minimum salary cannot be negative",
                        field = "minSalary"
                    )
                )
                maxSalary != null && maxSalary < 0 -> Result.Error(
                    AppError.ValidationError(
                        message = "Maximum salary cannot be negative",
                        field = "maxSalary"
                    )
                )
                minSalary != null && maxSalary != null && minSalary > maxSalary -> Result.Error(
                    AppError.ValidationError(
                        message = "Minimum salary cannot be greater than maximum salary",
                        field = "salary"
                    )
                )
                else -> Result.Success(this)
            }
        }
    }

    override suspend fun execute(parameters: Params): Flow<Result<List<Job>>> {
        val validationResult = parameters.validate()
        if (validationResult is Result.Error) {
            return flowOf(validationResult)
        }

        return jobRepository.getJobs(
            page = parameters.page,
            pageSize = parameters.pageSize,
            state = parameters.state,
            district = parameters.district,
            radius = parameters.radius,
            latitude = parameters.latitude,
            longitude = parameters.longitude,
            minSalary = parameters.minSalary,
            maxSalary = parameters.maxSalary
        ).map { result ->
            when (result) {
                is Result.Success -> Result.Success(result.data)
                is Result.Error -> result
                is Result.Loading -> Result.Loading
            }
        }
    }
}

class ValidateEmailUseCase @Inject constructor() : SyncUseCase<String, Boolean>() {
    override fun execute(parameters: String): Result<Boolean> {
        return try {
            if (parameters.isBlank()) {
                Result.Error(AppError.ValidationError(
                    message = "Email cannot be empty",
                    field = "email"
                ))
            } else if (!Patterns.EMAIL_ADDRESS.matcher(parameters).matches()) {
                Result.Error(AppError.ValidationError(
                    message = "Invalid email format",
                    field = "email"
                ))
            } else {
                Result.Success(true)
            }
        } catch (e: Exception) {
            Result.Error(AppError.UnexpectedError(
                message = "Error validating email",
                cause = e
            ))
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowNoParamsUseCase<UserProfile>(dispatcher) {

    override suspend fun execute(): Flow<Result<UserProfile>> {
        return userRepository.getUserProfile()
            .map { result ->
                when (result) {
                    is Result.Success -> {
                        if (result.data.isProfileComplete()) {
                            Result.Success(result.data)
                        } else {
                            Result.Error(AppError.ValidationError(
                                message = "Profile is incomplete",
                                field = "profile"
                            ))
                        }
                    }
                    is Result.Error -> result
                    is Result.Loading -> Result.Loading
                }
            }
    }

    private fun UserProfile.isProfileComplete(): Boolean {
        return !name.isNullOrBlank() &&
                !dateOfBirth.isNullOrBlank() &&
                !gender.isNullOrBlank() &&
                currentLocation != null
    }
}

// Helper function to create flow of result
private fun <T> flowOf(result: Result<T>): Flow<Result<T>> = flow {
    emit(result)
}