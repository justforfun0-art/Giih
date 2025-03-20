package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.usecase.user.GetUserUseCase
import com.example.gigwork.domain.usecase.user.UpdateUserProfileUseCase
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val updateProfileUseCase: UpdateUserProfileUseCase,
    private val logger: Logger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "UserViewModel"
        private const val KEY_USER_ID = "user_id"
    }

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Initial)
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        logger.d(
            tag = TAG,
            message = "Initializing UserViewModel",
            additionalData = mapOf(
                "saved_user_id" to savedStateHandle.get<String>(KEY_USER_ID),
                "timestamp" to System.currentTimeMillis()
            )
        )
    }

    fun loadUserData(userId: String) {
        if (userId.isBlank()) {
            handleError(AppError.ValidationError(message = "User ID cannot be empty"))
            return
        }

        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()

                logger.d(
                    tag = TAG,
                    message = "Loading user data",
                    additionalData = mapOf(
                        "user_id" to userId,
                        "current_state" to _uiState.value.toString()
                    )
                )

                getUserUseCase(userId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> handleUserLoadSuccess(result.data)
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> setLoading(true)
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                setLoading(false)
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        if (!validateProfile(profile)) {
            return
        }

        viewModelScope.launch {
            try {
                setLoading(true)
                clearError()

                logger.d(
                    tag = TAG,
                    message = "Updating user profile",
                    additionalData = mapOf(
                        "user_id" to profile.userId,
                        "fields_updated" to getUpdatedFields(profile)
                    )
                )

                val params = UpdateUserProfileUseCase.Params(profile)
                updateProfileUseCase(params).collect { result ->
                    when (result) {
                        is ApiResult.Success -> handleProfileUpdateSuccess(profile.userId)
                        is ApiResult.Error -> handleError(result.error)
                        is ApiResult.Loading -> setLoading(true)
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun handleUserLoadSuccess(user: User) {
        logger.i(
            tag = TAG,
            message = "Successfully loaded user data",
            additionalData = mapOf(
                "user_id" to user.id,
                "has_profile" to (user.profile != null)
            )
        )
        _uiState.value = UserUiState.Success(user)
        setLoading(false)
        clearError()
    }

    private fun handleProfileUpdateSuccess(userId: String) {
        logger.i(
            tag = TAG,
            message = "Successfully updated user profile",
            additionalData = mapOf("user_id" to userId)
        )
        loadUserData(userId)
    }

    private fun handleError(error: AppError) {
        logger.e(
            tag = TAG,
            message = "Error occurred",
            throwable = error,
            additionalData = mapOf("error_type" to error::class.simpleName)
        )
        _error.value = error.getUserMessage()
        _uiState.value = UserUiState.Error(error.getUserMessage())
        setLoading(false)
    }

    private fun handleException(exception: Exception) {
        logger.e(
            tag = TAG,
            message = "Unexpected error occurred",
            throwable = exception
        )
        val errorMessage = exception.message ?: "An unexpected error occurred"
        _error.value = errorMessage
        _uiState.value = UserUiState.Error(errorMessage)
        setLoading(false)
    }

    private fun validateProfile(profile: UserProfile): Boolean {
        if (profile.userId.isBlank()) {
            handleError(AppError.ValidationError(message = "User ID cannot be empty"))
            return false
        }
        return true
    }

    private fun getUpdatedFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "name" to profile.name,
            "date_of_birth" to profile.dateOfBirth,
            "gender" to profile.gender,
            "current_location" to profile.currentLocation,
            "preferred_location" to profile.preferredLocation,
            "qualification" to profile.qualification,
            "computer_knowledge" to profile.computerKnowledge,
            "photo" to profile.photo,
            "company_name" to profile.companyName,
            "company_function" to profile.companyFunction,
            "staff_count" to profile.staffCount,
            "yearly_turnover" to profile.yearlyTurnover
        ).filterValues { it != null }
    }

    private fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    private fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        logger.d(
            tag = TAG,
            message = "UserViewModel being cleared",
            additionalData = mapOf(
                "final_state" to _uiState.value.toString(),
                "has_error" to (_error.value != null)
            )
        )
        super.onCleared()
    }
}

sealed class UserUiState {
    object Initial : UserUiState()
    object Loading : UserUiState()
    data class Success(val user: User) : UserUiState()
    data class Error(val message: String) : UserUiState()

    override fun toString(): String = when (this) {
        is Initial -> "Initial"
        is Loading -> "Loading"
        is Success -> "Success(userId=${user.id})"
        is Error -> "Error(message=$message)"
    }
}