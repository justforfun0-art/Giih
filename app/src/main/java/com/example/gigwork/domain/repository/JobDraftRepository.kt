package com.example.gigwork.domain.repository

import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.presentation.states.JobDraft
import kotlinx.coroutines.flow.Flow

interface JobDraftRepository {
    suspend fun saveDraft(draft: JobDraft): Flow<ApiResult<Unit>>
    suspend fun getDraft(id: String): Flow<ApiResult<JobDraft?>>
    suspend fun deleteDraft(id: String): Flow<ApiResult<Unit>>
    suspend fun getAllDrafts(): Flow<ApiResult<List<JobDraft>>>
    suspend fun getDraftCount(): Flow<ApiResult<Int>>
    suspend fun clearAllDrafts(): Flow<ApiResult<Unit>>
    suspend fun searchDrafts(query: String): Flow<ApiResult<List<JobDraft>>>
    suspend fun getDraftsByIds(ids: List<String>): Flow<ApiResult<List<JobDraft>>>
    suspend fun updateDraft(draft: JobDraft): Flow<ApiResult<Unit>>
    suspend fun deleteOldDrafts(timestamp: Long): Flow<ApiResult<Unit>>
    suspend fun cleanupOldDrafts(maxAgeInMillis: Long): Flow<ApiResult<Unit>>




}