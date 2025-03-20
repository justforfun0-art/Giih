package com.example.gigwork.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.database.JobDraftDao
import com.example.gigwork.data.database.JobDraftEntity
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
        private const val MIN_DRAFT_ID_LENGTH = 1
        private const val MIN_TITLE_LENGTH = 1
        private const val MIN_DESCRIPTION_LENGTH = 1
    }

    override suspend fun saveDraft(draft: JobDraft): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            validateDraft(draft)
            withContext(ioDispatcher) {
                jobDraftDao.insertDraft(draft.toEntity())
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "Draft saved successfully", mapOf(
                "draftId" to draft.id,
                "title" to draft.title
            ))
        } catch (e: SQLiteConstraintException) {
            logger.e(TAG, "Database constraint violation", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Draft already exists or constraint violation",
                cause = e,
                entity = "job_drafts",
                operation = "insert",
                sqlError = e.message
            )))
        } catch (e: SQLiteException) {
            logger.e(TAG, "SQLite error", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Database error while saving draft",
                cause = e,
                entity = "job_drafts",
                operation = "insert",
                sqlError = e.message
            )))
        } catch (e: IllegalArgumentException) {
            logger.e(TAG, "Validation error", e)
            emit(ApiResult.Error(AppError.ValidationError(
                message = e.message ?: "Invalid draft data",
                field = "draft",
                value = draft
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Unexpected error", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Unexpected error while saving draft",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun getDraft(id: String): Flow<ApiResult<JobDraft?>> = flow {
        emit(ApiResult.Loading)
        try {
            if (id.isBlank()) {
                throw IllegalArgumentException("Draft ID cannot be blank")
            }

            val draft = withContext(ioDispatcher) {
                jobDraftDao.getDraft(id)?.toDomain()
            }
            emit(ApiResult.Success(draft))
            logger.d(TAG, "Draft retrieved", mapOf(
                "draftId" to id,
                "found" to (draft != null)
            ))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error retrieving draft", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to retrieve draft",
                cause = e,
                entity = "job_drafts",
                operation = "query",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error retrieving draft", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to retrieve draft",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun deleteDraft(id: String): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            if (id.isBlank()) {
                throw IllegalArgumentException("Draft ID cannot be blank")
            }

            withContext(ioDispatcher) {
                jobDraftDao.deleteDraft(id)
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "Draft deleted", mapOf("draftId" to id))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error deleting draft", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to delete draft",
                cause = e,
                entity = "job_drafts",
                operation = "delete",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting draft", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to delete draft",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun getAllDrafts(): Flow<ApiResult<List<JobDraft>>> = flow {
        emit(ApiResult.Loading)
        try {
            jobDraftDao.getAllDrafts()
                .catch { e ->
                    logger.e(TAG, "Error in drafts flow", e)
                    emit(ApiResult.Error(AppError.DatabaseError(
                        message = "Error retrieving drafts",
                        cause = e,
                        entity = "job_drafts",
                        operation = "query"
                    )))
                }
                .map { entities ->
                    entities.mapNotNull { entity ->
                        try {
                            entity.toDomain()
                        } catch (e: Exception) {
                            logger.w(TAG, "Failed to map draft entity", e, mapOf(
                                "entityId" to entity.id
                            ))
                            null
                        }
                    }
                }
                .collect { drafts ->
                    emit(ApiResult.Success(drafts))
                    logger.d(TAG, "Retrieved all drafts", mapOf(
                        "count" to drafts.size
                    ))
                }
        } catch (e: Exception) {
            logger.e(TAG, "Error retrieving all drafts", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to retrieve drafts",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun getDraftCount(): Flow<ApiResult<Int>> = flow {
        emit(ApiResult.Loading)
        try {
            val count = withContext(ioDispatcher) {
                jobDraftDao.getDraftCount()
            }
            emit(ApiResult.Success(count))
            logger.d(TAG, "Draft count retrieved", mapOf("count" to count))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error getting draft count", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to get draft count",
                cause = e,
                entity = "job_drafts",
                operation = "count",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error getting draft count", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to get draft count",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun clearAllDrafts(): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            withContext(ioDispatcher) {
                jobDraftDao.clearAllDrafts()
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "All drafts cleared")
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error clearing drafts", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to clear drafts",
                cause = e,
                entity = "job_drafts",
                operation = "clear",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error clearing all drafts", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to clear drafts",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    private fun validateDraft(draft: JobDraft) {
        require(draft.id.length >= MIN_DRAFT_ID_LENGTH) {
            "Draft ID must not be blank"
        }
        require(draft.title.length >= MIN_TITLE_LENGTH) {
            "Draft title must not be blank"
        }
        require(draft.description.length >= MIN_DESCRIPTION_LENGTH) {
            "Draft description must not be blank"
        }
        require(draft.salary >= 0) {
            "Salary must be non-negative"
        }
        require(draft.workDuration > 0) {
            "Work duration must be positive"
        }
    }

    override suspend fun searchDrafts(query: String): Flow<ApiResult<List<JobDraft>>> = flow {
        emit(ApiResult.Loading)
        try {
            jobDraftDao.searchDrafts(query)
                .catch { e ->
                    logger.e(TAG, "Error in search flow", e)
                    emit(ApiResult.Error(AppError.DatabaseError(
                        message = "Error searching drafts",
                        cause = e,
                        entity = "job_drafts",
                        operation = "search"
                    )))
                }
                .map { entities ->
                    entities.mapNotNull { entity ->
                        try {
                            entity.toDomain()
                        } catch (e: Exception) {
                            logger.w(TAG, "Failed to map draft entity", e, mapOf(
                                "entityId" to entity.id
                            ))
                            null
                        }
                    }
                }
                .collect { drafts ->
                    emit(ApiResult.Success(drafts))
                    logger.d(TAG, "Search completed", mapOf(
                        "query" to query,
                        "resultsCount" to drafts.size
                    ))
                }
        } catch (e: Exception) {
            logger.e(TAG, "Error searching drafts", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to search drafts",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun getDraftsByIds(ids: List<String>): Flow<ApiResult<List<JobDraft>>> = flow {
        emit(ApiResult.Loading)
        try {
            if (ids.isEmpty()) {
                emit(ApiResult.Success(emptyList()))
                return@flow
            }

            val drafts = withContext(ioDispatcher) {
                jobDraftDao.getDraftsByIds(ids)
                    .mapNotNull { entity ->
                        try {
                            entity.toDomain()
                        } catch (e: Exception) {
                            logger.w(TAG, "Failed to map draft entity", e, mapOf(
                                "entityId" to entity.id
                            ))
                            null
                        }
                    }
            }
            emit(ApiResult.Success(drafts))
            logger.d(TAG, "Retrieved drafts by IDs", mapOf(
                "requestedCount" to ids.size,
                "foundCount" to drafts.size
            ))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error retrieving drafts by IDs", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to retrieve drafts by IDs",
                cause = e,
                entity = "job_drafts",
                operation = "query",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error retrieving drafts by IDs", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to retrieve drafts by IDs",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun updateDraft(draft: JobDraft): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            validateDraft(draft)
            withContext(ioDispatcher) {
                jobDraftDao.updateDraft(draft.toEntity())
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "Draft updated successfully", mapOf(
                "draftId" to draft.id
            ))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error updating draft", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to update draft",
                cause = e,
                entity = "job_drafts",
                operation = "update",
                sqlError = e.message
            )))
        } catch (e: IllegalArgumentException) {
            logger.e(TAG, "Validation error", e)
            emit(ApiResult.Error(AppError.ValidationError(
                message = e.message ?: "Invalid draft data",
                field = "draft",
                value = draft
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error updating draft", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to update draft",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

    override suspend fun deleteOldDrafts(timestamp: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            require(timestamp > 0) { "Timestamp must be positive" }

            withContext(ioDispatcher) {
                jobDraftDao.deleteOldDrafts(timestamp)
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "Old drafts deleted", mapOf(
                "timestamp" to timestamp
            ))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error deleting old drafts", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to delete old drafts",
                cause = e,
                entity = "job_drafts",
                operation = "delete",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error deleting old drafts", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to delete old drafts",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }

                override suspend fun cleanupOldDrafts(maxAgeInMillis: Long): Flow<ApiResult<Unit>> = flow {
        emit(ApiResult.Loading)
        try {
            require(maxAgeInMillis > 0) { "Max age must be positive" }

            withContext(ioDispatcher) {
                jobDraftDao.cleanupOldDrafts(maxAgeInMillis)
            }
            emit(ApiResult.Success(Unit))
            logger.d(TAG, "Old drafts cleaned up", mapOf(
                "maxAgeInMillis" to maxAgeInMillis
            ))
        } catch (e: SQLiteException) {
            logger.e(TAG, "Database error cleaning up old drafts", e)
            emit(ApiResult.Error(AppError.DatabaseError(
                message = "Failed to clean up old drafts",
                cause = e,
                entity = "job_drafts",
                operation = "cleanup",
                sqlError = e.message
            )))
        } catch (e: Exception) {
            logger.e(TAG, "Error cleaning up old drafts", e)
            emit(ApiResult.Error(AppError.UnexpectedError(
                message = "Failed to clean up old drafts",
                cause = e,
                stackTrace = e.stackTraceToString()
            )))
        }
    }
}

// Extension functions for mapping between Entity and Domain models
 fun JobDraftEntity.toDomain(): JobDraft = try {
    JobDraft(
        id = id.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invalid ID"),
        title = title.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invalid title"),
        description = description.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invalid description"),
        salary = salary.takeIf { it >= 0 }
            ?: throw IllegalStateException("Invalid salary"),
        salaryUnit = salaryUnit.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invalid salary unit"),
        workDuration = workDuration.takeIf { it > 0 }
            ?: throw IllegalStateException("Invalid work duration"),
        workDurationUnit = workDurationUnit.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Invalid work duration unit"),
        location = location,
        lastModified = lastModified
    )
} catch (e: Exception) {
    throw IllegalStateException("Failed to map JobDraftEntity to JobDraft", e)
}

private fun JobDraft.toEntity() = JobDraftEntity(
    id = id,
    title = title,
    description = description,
    salary = salary,
    salaryUnit = salaryUnit,
    workDuration = workDuration,
    workDurationUnit = workDurationUnit,
    location = location,
    lastModified = lastModified
)
