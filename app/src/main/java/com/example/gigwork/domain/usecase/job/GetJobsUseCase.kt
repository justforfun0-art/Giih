package com.example.gigwork.domain.usecase.job

import android.database.SQLException
import android.util.Patterns
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.UserRepository
import com.example.gigwork.domain.usecase.base.FlowUseCase
import com.example.gigwork.domain.usecase.base.SyncUseCase
import com.example.gigwork.domain.usecase.base.FlowNoParamsUseCase
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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

        fun validate(): ApiResult<Params> {
            return when {
                page < 1 -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Page number must be greater than 0",
                        field = "page"
                    )
                )
                pageSize < 1 -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Page size must be greater than 0",
                        field = "pageSize"
                    )
                )
                radius != null && (latitude == null || longitude == null) -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Latitude and longitude are required when radius is specified",
                        field = "location"
                    )
                )
                radius != null && radius <= 0 -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Radius must be greater than 0",
                        field = "radius"
                    )
                )
                minSalary != null && minSalary < 0 -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Minimum salary cannot be negative",
                        field = "minSalary"
                    )
                )
                maxSalary != null && maxSalary < 0 -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Maximum salary cannot be negative",
                        field = "maxSalary"
                    )
                )
                minSalary != null && maxSalary != null && minSalary > maxSalary -> ApiResult.Error(
                    AppError.ValidationError(
                        message = "Minimum salary cannot be greater than maximum salary",
                        field = "salary"
                    )
                )
                else -> ApiResult.Success(this)
            }
        }
    }

    override suspend fun execute(parameters: Params): Flow<List<Job>> = flow {
        when (val validationResult = parameters.validate()) {
            is ApiResult.Success<Params> -> {
                jobRepository.getJobs(
                    page = validationResult.data.page,
                    pageSize = validationResult.data.pageSize,
                    state = validationResult.data.state,
                    district = validationResult.data.district,
                    radius = validationResult.data.radius,
                    latitude = validationResult.data.latitude,
                    longitude = validationResult.data.longitude,
                    minSalary = validationResult.data.minSalary,
                    maxSalary = validationResult.data.maxSalary
                ).collect { result ->
                    when (result) {
                        is ApiResult.Success<List<Job>> -> emit(result.data)
                        is ApiResult.Error -> throw convertToThrowable(result.error)
                        is ApiResult.Loading -> {
                            // Loading state handled by base class
                        }
                    }
                }
            }
            is ApiResult.Error -> throw convertToThrowable(validationResult.error)
            is ApiResult.Loading -> {
                // Loading state handled by base class
            }
        }
    }

    private fun convertToThrowable(error: AppError): Throwable {
        return when (error) {
            is AppError.ValidationError -> IllegalArgumentException(error.message)
            is AppError.NetworkError -> IOException(error.message)
            is AppError.DatabaseError -> SQLException(error.message)
            else -> Exception(error.message)
        }
    }
}

class ValidateEmailUseCase @Inject constructor() : SyncUseCase<String, Boolean>() {
    override fun execute(parameters: String): Boolean {
        if (parameters.isBlank()) {
            throw ValidationException(
                AppError.ValidationError(
                    message = "Email cannot be empty",
                    field = "email"
                )
            )
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(parameters).matches()) {
            throw ValidationException(
                AppError.ValidationError(
                    message = "Invalid email format",
                    field = "email"
                )
            )
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private class ValidationException(val error: AppError.ValidationError) : Exception(error.message)
}
class GetUserProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowNoParamsUseCase<UserProfile>(dispatcher) {

    override suspend fun execute(): Flow<UserProfile> = flow {
        userRepository.getUserProfile().collect { apiResult: ApiResult<UserProfile> ->
            when (apiResult) {
                is ApiResult.Success -> {
                    if (isProfileComplete(apiResult.data)) {
                        emit(apiResult.data)
                    } else {
                        throw ValidationException(
                            AppError.ValidationError(
                                message = "Profile is incomplete",
                                field = "profile"
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    throw ErrorWrapper(apiResult.error)
                }
                is ApiResult.Loading -> {
                    // Loading state handled by base class
                }
            }
        }
    }

    // GetUserProfileUseCase.kt


    private fun isProfileComplete(profile: UserProfile): Boolean {
        return !profile.name.isBlank() &&
                !profile.dateOfBirth.isNullOrBlank() &&
                !profile.gender.isNullOrBlank() &&
                profile.currentLocation != null
    }

    private class ValidationException(val error: AppError.ValidationError) : Exception(error.message)
    private class ErrorWrapper(val error: AppError) : Exception(error.message)
}

private fun <T> flowOf(result: ApiResult<T>): Flow<ApiResult<T>> = flow {
    emit(result)
}