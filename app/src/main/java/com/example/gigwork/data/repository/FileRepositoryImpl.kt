package com.example.gigwork.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.gigwork.core.error.ExceptionMapper
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.data.api.SupabaseClient
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.repository.FileRepository
import com.example.gigwork.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FileRepository {

    companion object {
        private const val TAG = "FileRepository"
        private const val BUCKET_NAME = "gigwork-files"
        private const val MAX_FILE_SIZE_MB = 10
        private const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024
    }

    override suspend fun uploadFile(uri: Uri): Flow<ApiResult<String>> = flow {
        emit(ApiResult.Loading)
        try {
            // Validate file size
            val fileSize = getFileSize(uri)
            if (fileSize > MAX_FILE_SIZE_BYTES) {
                logger.w(
                    tag = TAG,
                    message = "File too large",
                    additionalData = mapOf(
                        "file_size" to fileSize,
                        "max_size" to MAX_FILE_SIZE_BYTES
                    )
                )
                emit(ApiResult.Error(ExceptionMapper.map(
                    IllegalArgumentException("File size exceeds maximum limit of $MAX_FILE_SIZE_MB MB"),
                    "UPLOAD_FILE_TOO_LARGE"
                )))
                return@flow
            }

            // Get file MIME type
            val mimeType = getMimeType(uri) ?: "application/octet-stream"

            // Create unique filename
            val fileExtension = getFileExtension(uri)
            val fileName = "${UUID.randomUUID()}.$fileExtension"

            // Upload to Supabase Storage
            withContext(ioDispatcher) {
                try {
                    // Create a temporary file
                    val tempFile = createTempFile(uri)

                    // Read file as ByteArray
                    val fileBytes = tempFile.readBytes()

                    // Upload the file using ByteArray
                    supabaseClient.client.storage.from(BUCKET_NAME)
                        .upload(
                            path = fileName,
                            data = fileBytes)
                        {
                            upsert = false
                        }

                    // Delete the temporary file
                    tempFile.delete()

                    // Get public URL
                    val fileUrl = supabaseClient.client.storage.from(BUCKET_NAME)
                        .publicUrl(fileName)

                    logger.i(
                        tag = TAG,
                        message = "File uploaded successfully",
                        additionalData = mapOf(
                            "file_name" to fileName,
                            "file_size" to fileSize,
                            "mime_type" to mimeType
                        )
                    )

                    emit(ApiResult.Success(fileUrl))
                } catch (e: Exception) {
                    logger.e(
                        tag = TAG,
                        message = "Error uploading file to storage",
                        throwable = e,
                        additionalData = mapOf(
                            "file_name" to fileName,
                            "mime_type" to mimeType
                        )
                    )
                    emit(ApiResult.Error(ExceptionMapper.map(e, "UPLOAD_FILE_STORAGE")))
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error uploading file",
                throwable = e
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "UPLOAD_FILE")))
        }
    }

    override fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex("_size")
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    cursor.getLong(sizeIndex)
                } else {
                    // Fallback: try to get size from input stream
                    context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
                }
            } ?: 0L
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting file size",
                throwable = e,
                additionalData = mapOf("uri" to uri.toString())
            )
            0L
        }
    }

    override suspend fun deleteFile(fileUrl: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            // Extract file path from URL
            val fileName = extractFileNameFromUrl(fileUrl)
            if (fileName.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(
                    IllegalArgumentException("Invalid file URL"),
                    "DELETE_FILE_INVALID_URL"
                )))
                return@flow
            }

            // Delete from Supabase Storage
            withContext(ioDispatcher) {
                try {
                    supabaseClient.client.storage.from(BUCKET_NAME)
                        .delete(fileName)

                    logger.i(
                        tag = TAG,
                        message = "File deleted successfully",
                        additionalData = mapOf("file_name" to fileName)
                    )

                    emit(ApiResult.Success(true))
                } catch (e: Exception) {
                    logger.e(
                        tag = TAG,
                        message = "Error deleting file from storage",
                        throwable = e,
                        additionalData = mapOf("file_name" to fileName)
                    )
                    emit(ApiResult.Error(ExceptionMapper.map(e, "DELETE_FILE_STORAGE")))
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error deleting file",
                throwable = e,
                additionalData = mapOf("file_url" to fileUrl)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "DELETE_FILE")))
        }
    }

    override suspend fun getFileMetadata(fileUrl: String): Flow<ApiResult<FileRepository.FileMetadata>> = flow {
        emit(ApiResult.Loading)
        try {
            // Extract file path from URL
            val fileName = extractFileNameFromUrl(fileUrl)
            if (fileName.isBlank()) {
                emit(ApiResult.Error(ExceptionMapper.map(
                    IllegalArgumentException("Invalid file URL"),
                    "GET_FILE_METADATA_INVALID_URL"
                )))
                return@flow
            }

            // Get file information from Supabase Storage
            withContext(ioDispatcher) {
                try {
                    // We'll try to get a public URL - if this works, the file exists
                    val publicUrl = supabaseClient.client.storage.from(BUCKET_NAME)
                        .publicUrl(fileName)

                    // Create metadata with information we can derive or assume
                    val mimeType = getMimeTypeFromFileName(fileName)
                    val lastModified = System.currentTimeMillis()

                    val fileMetadata = FileRepository.FileMetadata(
                        name = fileName,
                        size = -1L, // Size not directly available without downloading
                        mimeType = mimeType,
                        url = publicUrl,
                        uploadedAt = lastModified,
                        metadata = mapOf(
                            "lastModified" to lastModified.toString()
                        )
                    )

                    logger.d(
                        tag = TAG,
                        message = "File metadata constructed",
                        additionalData = mapOf("file_name" to fileName)
                    )

                    emit(ApiResult.Success(fileMetadata))
                } catch (e: Exception) {
                    // If we get an exception trying to get the public URL, the file likely doesn't exist
                    logger.e(
                        tag = TAG,
                        message = "Error getting file metadata",
                        throwable = e,
                        additionalData = mapOf("file_name" to fileName)
                    )
                    emit(ApiResult.Error(ExceptionMapper.map(e, "GET_FILE_METADATA_STORAGE")))
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting file metadata",
                throwable = e,
                additionalData = mapOf("file_url" to fileUrl)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_FILE_METADATA")))
        }
    }

    override suspend fun getApplicationFiles(applicationId: String): Flow<ApiResult<List<FileRepository.FileMetadata>>> = flow {
        emit(ApiResult.Loading)
        try {
            // This would typically involve querying a database to get files associated with an application
            // This is a placeholder implementation
            withContext(ioDispatcher) {
                try {
                    // Placeholder - in a real implementation, you'd query for files associated with the application ID
                    val files = listOf<FileRepository.FileMetadata>()
                    emit(ApiResult.Success(files))
                } catch (e: Exception) {
                    logger.e(
                        tag = TAG,
                        message = "Error getting application files",
                        throwable = e,
                        additionalData = mapOf("application_id" to applicationId)
                    )
                    emit(ApiResult.Error(ExceptionMapper.map(e, "GET_APPLICATION_FILES")))
                }
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Error getting application files",
                throwable = e,
                additionalData = mapOf("application_id" to applicationId)
            )
            emit(ApiResult.Error(ExceptionMapper.map(e, "GET_APPLICATION_FILES")))
        }
    }

    // Helper methods

    private fun getMimeType(uri: Uri): String? {
        return if (uri.scheme == "content") {
            context.contentResolver.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.lowercase())
        }
    }

    private fun getFileExtension(uri: Uri): String {
        return if (uri.scheme == "content") {
            val mimeType = context.contentResolver.getType(uri)
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        } else {
            val path = uri.path
            path?.substringAfterLast('.', "bin") ?: "bin"
        }
    }

    private fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun extractFileNameFromUrl(fileUrl: String): String {
        // Extract the file name from the URL
        // This implementation depends on the URL structure of your storage service
        return fileUrl.substringAfterLast('/').substringBefore('?')
    }

    private suspend fun createTempFile(uri: Uri): File {
        return withContext(ioDispatcher) {
            val tempFile = File.createTempFile("upload", null, context.cacheDir)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4K buffer
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }

                    output.flush()
                }
            }

            tempFile
        }
    }
}