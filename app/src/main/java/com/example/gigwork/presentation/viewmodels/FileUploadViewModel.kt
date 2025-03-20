package com.example.gigwork.presentation.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.*
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.repository.FileRepository
import com.example.gigwork.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import com.example.gigwork.core.error.model.AppError as CoreAppError
import com.example.gigwork.presentation.base.AppError as BaseAppError

data class FileUploadUiState(
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val selectedFileUri: Uri? = null,
    val uploadedFileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val errorMessage: ErrorMessage? = null
)

sealed class FileUploadEvent {
    data class NavigateToPreview(val fileUrl: String) : FileUploadEvent()
    object ShowFilePicker : FileUploadEvent()
    data class UploadComplete(val fileUrl: String) : FileUploadEvent()
}

@HiltViewModel
class FileUploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileRepository: FileRepository,
    private val errorHandler: GlobalErrorHandler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB
        private const val TAG = "FileUploadViewModel"
        private val ALLOWED_MIME_TYPES = listOf(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "application/pdf"
        )
    }

    private val _uiState = MutableStateFlow(FileUploadUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FileUploadEvent>()
    val events = _events.asSharedFlow()

    fun selectFile(uri: Uri) {
        viewModelScope.launch {
            try {
                validateFile(uri)

                val fileName = getFileName(uri)
                val fileSize = getFileSize(uri)

                _uiState.update {
                    it.copy(
                        selectedFileUri = uri,
                        fileName = fileName,
                        fileSize = fileSize,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun validateFile(uri: Uri) {
        val mimeType = context.contentResolver.getType(uri)
        val fileSize = getFileSize(uri)

        when {
            mimeType == null -> {
                throw ValidationException("Invalid file type")
            }
            !ALLOWED_MIME_TYPES.contains(mimeType) -> {
                throw ValidationException("Unsupported file type. Allowed types: JPEG, PNG, PDF")
            }
            fileSize > MAX_FILE_SIZE -> {
                throw ValidationException("File size exceeds 5MB limit")
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun getFileSize(uri: Uri): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex)
        } ?: 0L
    }

    fun uploadFile() {
        viewModelScope.launch {
            try {
                val uri = uiState.value.selectedFileUri ?: throw ValidationException("No file selected")

                val networkUtils = NetworkUtils(context)
                if (!networkUtils.isNetworkAvailable()) {
                    throw NetworkException("No internet connection")
                }

                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                fileRepository.uploadFile(uri).collect { apiResult ->
                    when (apiResult) {
                        is ApiResult.Success -> handleUploadSuccess(apiResult.data)
                        is ApiResult.Error -> handleError(apiResult.error)
                        is ApiResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                            handleLoading()
                        }
                    }
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    private suspend fun handleUploadSuccess(fileUrl: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                progress = 1f,
                uploadedFileUrl = fileUrl,
                errorMessage = null
            )
        }
        _events.emit(FileUploadEvent.UploadComplete(fileUrl))
    }

    fun previewFile() {
        viewModelScope.launch {
            uiState.value.uploadedFileUrl?.let { url ->
                _events.emit(FileUploadEvent.NavigateToPreview(url))
            }
        }
    }

    fun showFilePicker() {
        viewModelScope.launch {
            _events.emit(FileUploadEvent.ShowFilePicker)
        }
    }

    private fun handleError(exception: Exception) {
        val error = when (exception) {
            is ValidationException -> AppError.ValidationError(
                message = exception.message ?: "Validation failed",
                field = "file",
                errorCode = "FILE_001"
            )
            is FileNotFoundException -> AppError.ValidationError(
                message = "File not found or inaccessible",
                field = "file",
                errorCode = "FILE_002"
            )
            is IOException -> AppError.NetworkError(
                message = "Failed to upload file",
                isConnectionError = true,
                errorCode = "FILE_003"
            )
            is NetworkException -> AppError.NetworkError(
                message = exception.message ?: "Network error occurred",
                isConnectionError = true,
                errorCode = "FILE_004"
            )
            else -> AppError.UnexpectedError(
                message = "An unexpected error occurred while uploading file",
                cause = exception,
                errorCode = "FILE_005"
            )
        }
        fun handleError(exception: Exception) {
            val error = when (exception) {
                is ValidationException -> CoreAppError.ValidationError(
                    message = exception.message ?: "Validation failed",
                    field = "file",
                    errorCode = "FILE_001",
                    constraints = listOf("File validation failed")
                )

                is FileNotFoundException -> CoreAppError.FileError(
                    message = "File not found or inaccessible",
                    errorCode = "FILE_002",
                    operation = CoreAppError.FileError.FileOperation.READ
                )

                is IOException -> CoreAppError.NetworkError(
                    message = "Failed to upload file",
                    isConnectionError = true,
                    errorCode = "FILE_003",
                    httpCode = null
                )

                is NetworkException -> CoreAppError.NetworkError(
                    message = exception.message ?: "Network error occurred",
                    isConnectionError = true,
                    errorCode = "FILE_004",
                    httpCode = null
                )

                else -> CoreAppError.UnexpectedError(
                    message = "An unexpected error occurred while uploading file",
                    cause = exception,
                    errorCode = "FILE_005"
                )
            }

            val baseError = when (error) {
                is CoreAppError.NetworkError -> BaseAppError.Network(
                    message = error.message,
                    cause = error.cause
                )
                is CoreAppError.ValidationError -> BaseAppError.Validation(
                    message = error.message,
                    errors = mapOf(error.field.orEmpty() to error.message)
                )
                is CoreAppError.FileError -> BaseAppError.UnexpectedError(
                    message = error.message,
                    cause = error.cause
                )
                else -> BaseAppError.UnexpectedError(
                    message = error.message,
                    cause = error.cause
                )
            }

            val errorMessage = errorHandler.handle(baseError)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> uploadFile()
            is ErrorAction.Custom -> {
                when (action.label) {
                    "Choose Different File" -> showFilePicker()
                    else -> clearError()
                }
            }
            else -> clearError()
        }
    }

    private fun handleLoading() {
        _uiState.update { it.copy(isLoading = true) }
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                progress = 0f
            )
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                isLoading = false,
                progress = 0f,
                selectedFileUri = null,
                uploadedFileUrl = null,
                fileName = null,
                fileSize = null,
                errorMessage = null
            )
        }
    }
}
data class FileUploadParams(
    val file: InputStream,
    val name: String,
    val type: String,
    val size: Long
)

class ValidationException(message: String) : Exception(message)
class NetworkException(message: String) : Exception(message)