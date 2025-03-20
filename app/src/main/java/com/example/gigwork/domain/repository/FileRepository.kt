package com.example.gigwork.domain.repository

import android.net.Uri
import com.example.gigwork.core.result.ApiResult
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    /**
     * Upload a file from URI
     * @param uri URI of the file to upload
     * @return Flow of ApiResult containing the uploaded file URL
     */
    suspend fun uploadFile(uri: Uri): Flow<ApiResult<String>>

    /**
     * Get file size from URI
     * @param uri URI of the file
     * @return Size of the file in bytes
     */
    fun getFileSize(uri: Uri): Long

    /**
     * Delete a file by URL
     * @param fileUrl URL of the file to delete
     * @return Flow of ApiResult indicating success or failure
     */
    suspend fun deleteFile(fileUrl: String): Flow<ApiResult<Boolean>>

    /**
     * Get metadata for a file
     * @param fileUrl URL of the file
     * @return Flow of ApiResult containing file metadata
     */
    suspend fun getFileMetadata(fileUrl: String): Flow<ApiResult<FileMetadata>>

    /**
     * Get list of files for a specific job application
     * @param applicationId ID of the job application
     * @return Flow of ApiResult containing list of file metadata
     */
    suspend fun getApplicationFiles(applicationId: String): Flow<ApiResult<List<FileMetadata>>>

    data class FileMetadata(
        val name: String,
        val size: Long,
        val mimeType: String,
        val url: String,
        val uploadedAt: Long,
        val metadata: Map<String, String> = emptyMap()
    )
}