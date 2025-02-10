// data/repository/JobDraftRepositoryImpl.kt
package com.example.gigwork.data.repository

import android.content.Context
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.Result
import com.example.gigwork.data.database.dao.JobDraftDao
import com.example.gigwork.data.database.entity.JobDraftEntity
import com.example.gigwork.domain.repository.JobDraftRepository
import com.example.gigwork.presentation.states.JobDraft
import com.example.gigwork.util.Logger
import com.example.gigwork.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobDraftRepositoryImpl @Inject constructor(
    private val jobDraftDao: JobDraftDao,
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : JobDraftRepository {

    companion object {
        private const val TAG = "JobDraftRepository"
    }

    override suspend fun saveDraft(draft: JobDraft): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            withContext(ioDispatcher) {
                jobDraftDao.insertDraft(draft.toEntity())
            }
            emit(Result.Success(Unit))
            logger.d(TAG, "Draft saved successfully", mapOf("draftId" to draft.id))
        } catch (e: Exception) {
            logger.e(TAG, "Error saving draft", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to save draft",
                cause = e,
                entity = "job_drafts",
                operation = "insert"
            )))
        }
    }

    override suspend fun getDraft(id: String): Flow<Result<JobDraft?>> = flow {
        emit(Result.Loading)
        try {
            val draft = withContext(ioDispatcher) {
                jobDraftDao.getDraft(id)?.toDomain()
            }
            emit(Result.Success(draft))
            logger.d(TAG, "Draft retrieved", mapOf("draftId" to id, "found" to (draft != null)))
        } catch (e: Exception) {
            logger.e(TAG, "Error retrieving draft", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to retrieve draft",
                cause = e,
                entity = "job_drafts",
                operation = "query"
            )))
        }
    }

    override suspend fun deleteDraft(id: String): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            withContext(ioDispatcher) {
                jobDraftDao.deleteDraft(id)
            }
            emit(Result.Success(Unit))
            logger.d(TAG, "Draft deleted", mapOf("draftId" to id))
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting draft", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to delete draft",
                cause = e,
                entity = "job_drafts",
                operation = "delete"
            )))
        }
    }

    override suspend fun getAllDrafts(): Flow<Result<List<JobDraft>>> = flow {
        emit(Result.Loading)
        try {
            withContext(ioDispatcher) {
                jobDraftDao.getAllDrafts()
                    .map { entities -> entities.map { it.toDomain() } }
                    .collect { drafts ->
                        emit(Result.Success(drafts))
                        logger.d(TAG, "Retrieved all drafts", mapOf("count" to drafts.size))
                    }
            }
        } catch (e: Exception) {
            logger.e(TAG, "Error retrieving all drafts", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to retrieve drafts",
                cause = e,
                entity = "job_drafts",
                operation = "query"
            )))
        }
    }

    override suspend fun getDraftCount(): Flow<Result<Int>> = flow {
        emit(Result.Loading)
        try {
            val count = withContext(ioDispatcher) {
                jobDraftDao.getDraftCount()
            }
            emit(Result.Success(count))
            logger.d(TAG, "Draft count retrieved", mapOf("count" to count))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting draft count", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to get draft count",
                cause = e,
                entity = "job_drafts",
                operation = "count"
            )))
        }
    }

    override suspend fun clearAllDrafts(): Flow<Result<Unit>> = flow {
        emit(Result.Loading)
        try {
            withContext(ioDispatcher) {
                jobDraftDao.clearAllDrafts()
            }
            emit(Result.Success(Unit))
            logger.d(TAG, "All drafts cleared")
        } catch (e: Exception) {
            logger.e(TAG, "Error clearing all drafts", e)
            emit(Result.Error(AppError.DatabaseError(
                message = "Failed to clear drafts",
                cause = e,
                entity = "job_drafts",
                operation = "clear"
            )))
        }
    }
}