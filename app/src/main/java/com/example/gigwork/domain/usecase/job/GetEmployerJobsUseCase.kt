package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.usecase.base.FlowUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import com.example.gigwork.presentation.viewmodels.JobFilters


class GetEmployerJobsUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<GetEmployerJobsUseCase.Params, List<Job>>(dispatcher) {

    data class Params(
        val searchQuery: String = "",
        val filters: JobFilters? = null,
        val employerId: String,
        val page: Int = 1,
        val pageSize: Int = 20
    )

    override suspend fun execute(parameters: Params): Flow<List<Job>> = flow {
        try {
            // Convert employer filters to repository parameters
            val status = parameters.filters?.status
            val minSalary = parameters.filters?.minSalary
            val maxSalary = parameters.filters?.maxSalary
            val location = parameters.filters?.location?.let { loc ->
                // Parse location string into state/district if needed
                val parts = loc.split(",")
                if (parts.size >= 2) {
                    Pair(parts[0].trim(), parts[1].trim())
                } else null
            }

            jobRepository.getEmployerJobs(
                employerId = parameters.employerId,
                searchQuery = parameters.searchQuery,
                status = status,
                minSalary = minSalary,
                maxSalary = maxSalary,
                state = location?.first,
                district = location?.second,
                page = parameters.page,
                pageSize = parameters.pageSize
            ).collect { result ->
                when (result) {
                    is ApiResult.Success<List<Job>> -> emit(result.data)
                    is ApiResult.Error -> throw convertToThrowable(result.error)
                    is ApiResult.Loading -> {
                        // Loading state handled by base class
                    }
                }
            }
        } catch (e: Exception) {
            throw convertToThrowable(
                AppError.UnexpectedError(
                    message = "Failed to load employer jobs: ${e.message}",
                    cause = e
                )
            )
        }
    }

    private fun convertToThrowable(error: AppError): Throwable {
        return when (error) {
            is AppError.ValidationError -> IllegalArgumentException(error.message)
            is AppError.NetworkError -> io.ktor.utils.io.errors.IOException(error.message)
            is AppError.DatabaseError -> android.database.SQLException(error.message)
            else -> Exception(error.message)
        }
    }
}