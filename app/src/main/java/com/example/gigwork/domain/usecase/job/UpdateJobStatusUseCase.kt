package com.example.gigwork.domain.usecase.job

import com.example.gigwork.domain.models.Job
import com.example.gigwork.domain.repository.JobRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class UpdateJobStatusUseCase @Inject constructor(
    private val repository: JobRepository
) {
    suspend operator fun invoke(jobId: String, status: String): Flow<Job> {
        require(status in listOf("OPEN", "CLOSED", "PENDING", "DELETED")) {
            "Invalid job status"
        }
        return repository.updateJobStatus(jobId, status)
    }
}