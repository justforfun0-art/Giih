// domain/repository/JobDraftRepository.kt
package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.Result
import com.example.gigwork.presentation.states.JobDraft
import kotlinx.coroutines.flow.Flow

interface JobDraftRepository {
    suspend fun saveDraft(draft: JobDraft): Flow<Result<Unit>>
    suspend fun getDraft(id: String): Flow<Result<JobDraft?>>
    suspend fun deleteDraft(id: String): Flow<Result<Unit>>
    suspend fun getAllDrafts(): Flow<Result<List<JobDraft>>>
    suspend fun getDraftCount(): Flow<Result<Int>>
    suspend fun clearAllDrafts(): Flow<Result<Unit>>
}