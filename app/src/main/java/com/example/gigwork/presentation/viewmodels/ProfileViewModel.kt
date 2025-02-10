package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import com.example.gigwork.core.error.handler.ErrorHandler
import com.example.gigwork.core.error.model.AppError
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.result.Result
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.usecase.user.CreateUserProfileUseCase
import com.example.gigwork.domain.usecase.user.UpdateUserProfileUseCase
import com.example.gigwork.presentation.screens.common.BaseErrorViewModel
import com.example.gigwork.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProfileState(
    val dateOfBirth: String = "",
    val gender: String = "",
    val currentLocation: String = "",
    val qualification: String = "",
    val computerKnowledge: String = "",
    val isSuccess: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    override val isLoading: Boolean = false,
    override val errorMessage: ErrorMessage? = null
) : UiState

sealed class ProfileEvent {
    data class ValidationError(val errors: Map<String, String>) : ProfileEvent()
    object ProfileUpdated : ProfileEvent()
    object ProfileCreated : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    errorHandler: ErrorHandler,
    private val updateProfileUseCase: UpdateUserProfileUseCase,
    private val createProfileUseCase: CreateUserProfileUseCase,
    logger: Logger
) : BaseErrorViewModel<ProfileState, ProfileEvent>(savedStateHandle, errorHandler, logger) {

    override fun createInitialState() = ProfileState()

    fun updateProfile(profile: UserProfile) = safeLaunch {
        try {
            updateProfileUseCase(UpdateUserProfileUseCase.Params(profile)).collect { result ->
                when (result) {
                    is Result.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                isSuccess = true,
                                errorMessage = null
                            )
                        }
                        emitEvent(ProfileEvent.ProfileUpdated)
                    }
                    is Result.Error -> {
                        handleError(AppError.UnexpectedError(
                            message = result.error.message ?: "Failed to update profile",
                            cause = result.error
                        ))
                    }
                    is Result.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(e)
            logError(
                tag = TAG,
                message = "Error updating profile",
                throwable = e
            )
        }
    }

    fun createProfile(userId: String, profile: UserProfile) = safeLaunch {
        if (!validateForm()) return@safeLaunch

        try {
            createProfileUseCase(userId, profile).collect { result ->
                when (result) {
                    is Result.Success -> {
                        setState {
                            copy(
                                isLoading = false,
                                isSuccess = true,
                                errorMessage = null
                            )
                        }
                        emitEvent(ProfileEvent.ProfileCreated)
                    }
                    is Result.Error -> {
                        handleError(AppError.UnexpectedError(
                            message = result.error.message ?: "Failed to create profile",
                            cause = result.error
                        ))
                    }
                    is Result.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                }
            }
        } catch (e: Exception) {
            handleError(e)
            logError(
                tag = TAG,
                message = "Error creating profile",
                throwable = e
            )
        }
    }

    fun updateDateOfBirth(dob: String) {
        setState { copy(dateOfBirth = dob) }
        validateForm()
    }

    fun updateGender(gender: String) {
        setState { copy(gender = gender) }
        validateForm()
    }

    fun updateLocation(location: String) {
        setState { copy(currentLocation = location) }
        validateForm()
    }

    fun updateQualification(qualification: String) {
        setState { copy(qualification = qualification) }
        validateForm()
    }

    fun updateComputerKnowledge(knowledge: String) {
        setState { copy(computerKnowledge = knowledge) }
        validateForm()
    }

    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val currentState = state.value

        with(currentState) {
            if (dateOfBirth.isBlank()) {
                errors["dateOfBirth"] = "Date of birth is required"
            }
            if (gender.isBlank()) {
                errors["gender"] = "Gender is required"
            }
            if (currentLocation.isBlank()) {
                errors["currentLocation"] = "Current location is required"
            }
            if (qualification.isBlank()) {
                errors["qualification"] = "Qualification is required"
            }
            if (computerKnowledge.isBlank()) {
                errors["computerKnowledge"] = "Computer knowledge is required"
            }
        }

        setState { copy(validationErrors = errors) }

        if (errors.isNotEmpty()) {
            safeLaunch {
                emitEvent(ProfileEvent.ValidationError(errors))
            }
            return false
        }
        return true
    }

    fun resetState() {
        setState { createInitialState() }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}