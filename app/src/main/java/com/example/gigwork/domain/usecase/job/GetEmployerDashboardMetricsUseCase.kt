package com.example.gigwork.domain.usecase.job

import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.DashboardMetrics
import com.example.gigwork.domain.repository.JobRepository
import com.example.gigwork.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetEmployerDashboardMetricsUseCase @Inject constructor(
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    operator fun invoke(): Flow<ApiResult<DashboardMetrics>> = flow {
        emit(ApiResult.Loading)

        try {
            // Get current user ID directly as a string, not a flow
            val userId = userRepository.getCurrentUserId()

            // Get dashboard metrics directly
            val metrics = jobRepository.getEmployerDashboardMetrics(userId)

            // Emit success with the metrics
            emit(ApiResult.Success(metrics))

        } catch (e: Exception) {
            emit(ApiResult.Error(
                AppError.UnexpectedError(
                    message = e.message ?: "Failed to load dashboard metrics",
                    cause = e
                )
            ))
        }
    }.flowOn(ioDispatcher)
}