package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gigwork.domain.models.User
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.gigwork.domain.usecase.user.GetUserUseCase
import com.example.gigwork.domain.usecase.user.UpdateUserProfileUseCase

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
    val uiState = _uiState.asStateFlow()

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
        logger.d(
            tag = TAG,
            message = "Loading user data",
            additionalData = mapOf(
                "user_id" to userId,
                "current_state" to _uiState.value.javaClass.simpleName
            )
        )

        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading

                val startTime = System.currentTimeMillis()
                getUserUseCase(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val duration = System.currentTimeMillis() - startTime
                            logger.i(
                                tag = TAG,
                                message = "Successfully loaded user data",
                                additionalData = mapOf(
                                    "user_id" to userId,
                                    "duration_ms" to duration,
                                    "has_profile" to (result.data.profile != null)
                                )
                            )
                            _uiState.value = UserUiState.Success(result.data)
                        }
                        is Result.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to load user data",
                                throwable = result.error,
                                additionalData = mapOf(
                                    "user_id" to userId,
                                    "error_type" to result.error.javaClass.simpleName
                                )
                            )
                            _uiState.value = UserUiState.Error(result.error.message)
                        }
                        is Result.Loading -> {
                            _uiState.value = UserUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Unexpected error loading user data",
                    throwable = e,
                    additionalData = mapOf(
                        "user_id" to userId,
                        "error_type" to e.javaClass.simpleName
                    )
                )
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateUserProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                _uiState.value = UserUiState.Loading

                logger.d(
                    tag = TAG,
                    message = "Updating user profile",
                    additionalData = mapOf(
                        "user_id" to profile.userId,
                        "fields_updated" to profile.getUpdatedFields()
                    )
                )

                updateProfileUseCase(profile).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            logger.i(
                                tag = TAG,
                                message = "Successfully updated user profile",
                                additionalData = mapOf(
                                    "user_id" to profile.userId,
                                    "update_time" to System.currentTimeMillis()
                                )
                            )
                            loadUserData(profile.userId)
                        }
                        is Result.Error -> {
                            logger.e(
                                tag = TAG,
                                message = "Failed to update user profile",
                                throwable = result.error,
                                additionalData = mapOf(
                                    "user_id" to profile.userId,
                                    "error_type" to result.error.javaClass.simpleName
                                )
                            )
                            _uiState.value = UserUiState.Error(result.error.message)
                        }
                        is Result.Loading -> {
                            _uiState.value = UserUiState.Loading
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Unexpected error updating user profile",
                    throwable = e,
                    additionalData = mapOf(
                        "user_id" to profile.userId,
                        "error_type" to e.javaClass.simpleName
                    )
                )
                _uiState.value = UserUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    override fun onCleared() {
        logger.d(
            tag = TAG,
            message = "UserViewModel being cleared",
            additionalData = mapOf(
                "final_state