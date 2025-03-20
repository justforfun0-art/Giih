package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.handler.GlobalErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.repository.AuthRepository
import com.example.gigwork.domain.usecase.user.GetUserProfileUseCase
import com.example.gigwork.domain.usecase.user.UpdateUserProfileUseCase
import com.example.gigwork.domain.usecase.user.ValidateProfileUseCase
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State class for the employee profile screen
 */
data class EmployeeProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val errorMessage: ErrorMessage? = null,
    val updatedFields: Map<String, Any?> = emptyMap()
)

/**
 * ViewModel for the employee profile screen
 */
@HiltViewModel
class EmployeeProfileViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val validateProfileUseCase: ValidateProfileUseCase,
    private val authRepository: AuthRepository,
    private val errorHandler: GlobalErrorHandler,
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "EmployeeProfileViewModel"
        private const val KEY_IS_EDIT_MODE = "isEditMode"
    }

    private val _uiState = MutableStateFlow(EmployeeProfileState())
    val uiState: StateFlow<EmployeeProfileState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EmployeeProfileEvent>()
    val events: SharedFlow<EmployeeProfileEvent> = _events.asSharedFlow()

    init {
        restoreState()
    }

    private fun restoreState() {
        _uiState.update { currentState ->
            currentState.copy(
                isEditMode = savedStateHandle.get<Boolean>(KEY_IS_EDIT_MODE) ?: false
            )
        }
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                getUserProfileUseCase(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    user = result.data,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to load profile",
                        cause = e
                    )
                )
            }
        }
    }

    fun toggleEditMode() {
        val newEditMode = !_uiState.value.isEditMode
        savedStateHandle[KEY_IS_EDIT_MODE] = newEditMode
        _uiState.update {
            it.copy(
                isEditMode = newEditMode,
                validationErrors = emptyMap()
            )
        }
    }

    fun updateProfileField(field: String, value: String) {
        _uiState.update { state ->
            state.copy(
                updatedFields = state.updatedFields + (field to value),
                validationErrors = state.validationErrors - field
            )
        }
        validateField(field, value)
    }

    private fun validateField(field: String, value: String) {
        viewModelScope.launch {
            try {
                val result = validateProfileUseCase.validateField(field, value)
                if (result is ApiResult.Error && result.error is AppError.ValidationError) {
                    _uiState.update { state ->
                        state.copy(
                            validationErrors = state.validationErrors +
                                    (field to result.error.message)
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Error validating field: $field",
                    throwable = e
                )
            }
        }
    }

    fun saveProfile() {
        val currentUser = _uiState.value.user ?: return
        val currentProfile = currentUser.profile ?: return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }

                // Create updated profile
                val updatedProfile = createUpdatedProfile(currentProfile)

                // Create params for use case
                val params = UpdateUserProfileUseCase.Params(updatedProfile)

                // Validate profile - we need to make sure the validation happens
                // before we send update request
                val validationResult = validateProfileUseCase(updatedProfile)
                if (validationResult is ApiResult.Error) {
                    handleValidationError(validationResult.error)
                    return@launch
                }

                // Update profile
                updateUserProfileUseCase(params).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    isEditMode = false,
                                    updatedFields = emptyMap(),
                                    errorMessage = null
                                )
                            }
                            loadProfile(currentUser.id)
                            _events.emit(EmployeeProfileEvent.ProfileUpdated)
                        }
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> _uiState.update { it.copy(isSaving = true) }
                    }
                }
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to save profile",
                        cause = e
                    )
                )
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun createUpdatedProfile(currentProfile: UserProfile): UserProfile {
        val updatedFields = _uiState.value.updatedFields

        return currentProfile.copy(
            name = updatedFields["name"] as? String ?: currentProfile.name,
            dateOfBirth = updatedFields["dateOfBirth"] as? String ?: currentProfile.dateOfBirth,
            gender = updatedFields["gender"] as? String ?: currentProfile.gender,
            qualification = updatedFields["qualification"] as? String ?: currentProfile.qualification,
            computerKnowledge = if (updatedFields["computerKnowledge"] != null) {
                (updatedFields["computerKnowledge"] as String).toBoolean()
            } else {
                currentProfile.computerKnowledge
            }
            // Add other fields as needed
        )
    }

    private fun handleValidationError(error: AppError) {
        if (error is AppError.ValidationError) {
            val fieldErrors = mutableMapOf<String, String>()
            fieldErrors[error.field ?: ""] = error.message

            _uiState.update {
                it.copy(
                    validationErrors = fieldErrors,
                    isSaving = false
                )
            }

            viewModelScope.launch {
                _events.emit(EmployeeProfileEvent.ValidationError(fieldErrors))
            }
        } else {
            handleError(error)
        }
    }

    fun onSettingsClick() {
        viewModelScope.launch {
            _events.emit(EmployeeProfileEvent.NavigateToSettings)
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            try {
                // Clear auth data to perform logout
                authRepository.clearAuthData()
                _events.emit(EmployeeProfileEvent.Logout)
            } catch (e: Exception) {
                handleError(
                    AppError.UnexpectedError(
                        message = "Failed to logout",
                        cause = e
                    )
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun handleErrorAction(action: ErrorAction) {
        when (action) {
            is ErrorAction.Retry -> {
                _uiState.value.user?.let { loadProfile(it.id) }
            }
            else -> clearError()
        }
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error in EmployeeProfileViewModel: ${error.message}",
            throwable = error,
            additionalData = mapOf(
                "user_id" to (_uiState.value.user?.id ?: "unknown"),
                "error_type" to error::class.simpleName
            )
        )

        val errorMessage = errorHandler.handleCoreError(error)
        _uiState.update {
            it.copy(
                isLoading = false,
                isSaving = false,
                errorMessage = errorMessage
            )
        }
    }
}