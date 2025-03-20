package com.example.gigwork.presentation.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.gigwork.core.error.model.AppError as CoreAppError
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.result.ApiResult
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.usecase.user.CreateUserProfileUseCase
import com.example.gigwork.domain.usecase.user.UpdateUserProfileUseCase
import com.example.gigwork.presentation.base.BaseErrorViewModel
import com.example.gigwork.presentation.base.ErrorHandler
import com.example.gigwork.presentation.base.Logger
import com.example.gigwork.presentation.base.UiEvent
import com.example.gigwork.presentation.base.UiState
import com.example.gigwork.di.BaseLogger
import com.example.gigwork.di.ErrorHandlerQualifier
import com.example.gigwork.domain.models.Location
import com.example.gigwork.util.Constants
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class ProfileState(
    val name: String = "",
    val phone: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val currentLocation: String = "",
    val locationState: String = "",
    val locationDistrict: String = "",
    val qualification: String = "",
    val computerKnowledge: String = "",

    val userType: String = Constants.UserType.EMPLOYEE,
    val email: String? = null,
    // Employer fields
    val companyName: String? = null,
    val companyFunction: String? = null,
    val staffCount: String? = null,
    val yearlyTurnover: String? = null,
    val isSuccess: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    override val isLoading: Boolean = false,
    override val errorMessage: ErrorMessage? = null
) : UiState<ProfileState> {
    override fun copy(
        isLoading: Boolean,
        errorMessage: ErrorMessage?
    ): ProfileState = copy(
        isLoading = isLoading,
        errorMessage = errorMessage,
        dateOfBirth = this.dateOfBirth,
        gender = this.gender,
        currentLocation = this.currentLocation,
        locationState = this.locationState,
        locationDistrict = this.locationDistrict,
        qualification = this.qualification,
        computerKnowledge = this.computerKnowledge,
        isSuccess = this.isSuccess,
        validationErrors = this.validationErrors
    )
}

sealed class ProfileEvent : UiEvent {
    data class ValidationError(val errors: Map<String, String>) : ProfileEvent()
    object ProfileUpdated : ProfileEvent()
    object ProfileCreated : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ErrorHandlerQualifier errorHandler: ErrorHandler,
    private val updateProfileUseCase: UpdateUserProfileUseCase,
    private val createProfileUseCase: CreateUserProfileUseCase,
    private val firebaseAuth: FirebaseAuth, // Inject this
    @BaseLogger logger: Logger
) : BaseErrorViewModel<ProfileState, ProfileEvent>(
    savedStateHandle = savedStateHandle,
    errorHandler = errorHandler,
    logger = logger
) {
    companion object {
        private const val TAG = "ProfileViewModel"
        private const val KEY_USER_TYPE = "user_type" // Define the constant
        // Other companion object constants...
    }
    override fun createInitialState() = ProfileState(
        dateOfBirth = getSavedState("dateOfBirth", ""),
        gender = getSavedState("gender", ""),
        currentLocation = getSavedState("currentLocation", ""),
        qualification = getSavedState("qualification", ""),
        computerKnowledge = getSavedState("computerKnowledge", "")
    )

    fun updateProfile(profile: UserProfile) {
        if (!validateForm()) return

        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, errorMessage = null) }

                val params = UpdateUserProfileUseCase.Params(profile)
                updateProfileUseCase(params)
                    .collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                setState {
                                    copy(
                                        isSuccess = true,
                                        isLoading = false,
                                        errorMessage = null
                                    )
                                }
                                emitEvent(ProfileEvent.ProfileUpdated)
                                clearSavedState()
                            }
                            is ApiResult.Error -> {
                                setState { copy(isLoading = false) }
                                handleCoreError(result.error)
                            }
                            is ApiResult.Loading -> {
                                setState { copy(isLoading = true, errorMessage = null) }
                            }
                        }
                    }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                handleException(e)
            }
        }
    }

    // Map name to fullName for compatibility
    fun updateFullName(name: String) {
        updateName(name)
    }

    // Add email handling
    fun updateEmail(email: String) {
        setState { copy(email = email) }
        setSavedState("email", email)
        validateForm()
    }

    // Add userType handling
    fun updateUserType(userType: String) {
        setState { copy(userType = userType) }
        setSavedState(KEY_USER_TYPE, userType)
        validateForm()
    }

    // Add save profile function
    fun saveProfile() {
        // Get current state values
        val currentState = state.value
        val userId = firebaseAuth.currentUser?.uid ?: return

        // Create a UserProfile
        val profile = UserProfile(
            id = "",
            userId = userId,
            name = currentState.name,
            photo = null,
            dateOfBirth = currentState.dateOfBirth,
            gender = currentState.gender,
            currentLocation = if (currentState.locationState.isNotEmpty() && currentState.locationDistrict.isNotEmpty()) {
                Location(
                    latitude = null,
                    longitude = null,
                    address = null,
                    pinCode = null,
                    state = currentState.locationState,
                    district = currentState.locationDistrict
                )
            } else null,
            preferredLocation = null,
            qualification = if (currentState.userType == Constants.UserType.EMPLOYEE) currentState.qualification else null,
            computerKnowledge = if (currentState.userType == Constants.UserType.EMPLOYEE) currentState.computerKnowledge.toBoolean() else null,
            aadharNumber = null,
            companyName = if (currentState.userType == Constants.UserType.EMPLOYER) currentState.companyName else null,
            companyFunction = if (currentState.userType == Constants.UserType.EMPLOYER) currentState.companyFunction else null,
            staffCount = if (currentState.userType == Constants.UserType.EMPLOYER) currentState.staffCount?.toIntOrNull() else null,
            yearlyTurnover = if (currentState.userType == Constants.UserType.EMPLOYER) currentState.yearlyTurnover else null
        )

        // Create the profile
        createProfile(userId, profile)
    }

    fun createProfile(userId: String, profile: UserProfile) {
        if (!validateForm()) return

        viewModelScope.launch {
            try {
                setState { copy(isLoading = true, errorMessage = null) }

                createProfileUseCase(userId, profile)
                    .collect { result ->
                        when (result) {
                            is ApiResult.Success -> {
                                setState {
                                    copy(
                                        isSuccess = true,
                                        isLoading = false,
                                        errorMessage = null
                                    )
                                }
                                emitEvent(ProfileEvent.ProfileCreated)
                                clearSavedState()
                            }
                            is ApiResult.Error -> {
                                setState { copy(isLoading = false) }
                                handleCoreError(result.error)
                            }
                            is ApiResult.Loading -> {
                                setState { copy(isLoading = true, errorMessage = null) }
                            }
                        }
                    }
            } catch (e: Exception) {
                setState { copy(isLoading = false) }
                handleException(e)
            }
        }
    }

    private fun handleCoreError(error: CoreAppError) {
        val errorMessage = when (error) {
            is CoreAppError.NetworkError -> ErrorMessage(
                message = error.message,
                title = "Network Error",
                level = if (error.isConnectionError) ErrorLevel.WARNING else ErrorLevel.ERROR
            )
            is CoreAppError.ValidationError -> ErrorMessage(
                message = error.message,
                title = "Validation Error",
                level = ErrorLevel.WARNING
            )
            is CoreAppError.DatabaseError -> ErrorMessage(
                message = error.message,
                title = "Database Error",
                level = ErrorLevel.ERROR
            )
            is CoreAppError.SecurityError -> ErrorMessage(
                message = error.message,
                title = "Security Error",
                level = ErrorLevel.ERROR
            )
            is CoreAppError.FileError -> ErrorMessage(
                message = error.message,
                title = "File Error",
                level = ErrorLevel.ERROR
            )
            is CoreAppError.CacheError -> ErrorMessage(
                message = error.message,
                title = "Cache Error",
                level = ErrorLevel.ERROR
            )
            is CoreAppError.BusinessError -> ErrorMessage(
                message = error.message,
                title = "Business Error",
                level = ErrorLevel.WARNING
            )
            is CoreAppError.UnexpectedError -> ErrorMessage(
                message = error.message,
                title = "Unexpected Error",
                level = ErrorLevel.CRITICAL
            )
            else -> ErrorMessage(
                message = error.message,
                title = "Error",
                level = ErrorLevel.ERROR
            )
        }

        setState { copy(errorMessage = errorMessage) }

        // If validation error, also emit validation event
        if (error is CoreAppError.ValidationError) {
            safeLaunch {
                emitEvent(ProfileEvent.ValidationError(
                    mapOf("validation" to error.message)
                ))
            }
        }
    }


    private fun handleException(exception: Exception) {
        val error = when (exception) {
            is CoreAppError -> handleCoreError(exception)
            else -> handleCoreError(
                CoreAppError.UnexpectedError(
                    message = exception.message ?: "Unknown error occurred",
                    cause = exception
                )
            )
        }
    }

    // Add these public methods
    fun onErrorDismiss() {
        setState { copy(errorMessage = null) }
    }

    fun onErrorAction(action: com.example.gigwork.core.error.model.ErrorAction) {
        when (action) {
            is com.example.gigwork.core.error.model.ErrorAction.Retry -> {
                // Implement retry logic if needed
            }
            is com.example.gigwork.core.error.model.ErrorAction.Dismiss -> {
                onErrorDismiss()
            }
            else -> Unit
        }
    }

    // If you need to load a profile, implement this method
    fun initializeProfile(userId: String, userType: String) = safeLaunch {
        // Save user type
        setSavedState(KEY_USER_TYPE, userType)

        // Get phone number from Firebase Auth
        val phoneNumber = firebaseAuth.currentUser?.phoneNumber ?: ""

        setState {
            copy(
                phone = phoneNumber,

                // Initialize different fields based on user type
                name = "",
                dateOfBirth = "",
                gender = "",
                locationState = "",
                locationDistrict = "",
                // For employees
                qualification = "",
                computerKnowledge = "",
                // For employers
                companyName = if (userType == Constants.UserType.EMPLOYER) "" else null,
                companyFunction = if (userType == Constants.UserType.EMPLOYER) "" else null,
                staffCount = if (userType == Constants.UserType.EMPLOYER) "" else null,
                yearlyTurnover = if (userType == Constants.UserType.EMPLOYER) "" else null
            )
        }
    }

    fun updateLocationState(state: String) {
        setState { copy(locationState = state) }
        setSavedState("locationState", state)
        validateForm()
    }

    fun updateLocationDistrict(district: String) {
        setState { copy(locationDistrict = district) }
        setSavedState("locationDistrict", district)
        validateForm()
    }

    fun updateDateOfBirth(dob: String) {
        setState { copy(dateOfBirth = dob) }
        setSavedState("dateOfBirth", dob)
        validateForm()
    }

    fun updateGender(gender: String) {
        setState { copy(gender = gender) }
        setSavedState("gender", gender)
        validateForm()
    }

    fun updateLocation(state: String, district: String) {
        setState {
            copy(
                locationState = state,
                locationDistrict = district,
                validationErrors = validationErrors - "location"
            )
        }
        validateForm()
    }

    fun updateQualification(qualification: String) {
        setState { copy(qualification = qualification) }
        setSavedState("qualification", qualification)
        validateForm()
    }

    fun updateComputerKnowledge(knowledge: String) {
        setState { copy(computerKnowledge = knowledge) }
        setSavedState("computerKnowledge", knowledge)
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

        setState {
            copy(
                validationErrors = errors,
                errorMessage = if (errors.isNotEmpty())
                    ErrorMessage(
                        message = "Please fill in all required fields",
                        level = ErrorLevel.WARNING
                    )
                else null
            )
        }

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
        clearAllSavedState()
    }

    override fun clearAllSavedState() {
        clearSavedState("dateOfBirth")
        clearSavedState("gender")
        clearSavedState("currentLocation")
        clearSavedState("qualification")
        clearSavedState("computerKnowledge")
    }

    fun updateName(name: String) {
        setState { copy(name = name) }
        setSavedState("name", name)
        validateForm()
    }

    fun updateCompanyName(name: String) {
        setState { copy(companyName = name) }
        setSavedState("companyName", name)
        validateForm()
    }

    fun updateCompanyFunction(function: String) {
        setState { copy(companyFunction = function) }
        setSavedState("companyFunction", function)
        validateForm()
    }

    fun updateStaffCount(count: String) {
        setState { copy(staffCount = count) }
        setSavedState("staffCount", count)
        validateForm()
    }

    fun updateYearlyTurnover(turnover: String) {
        setState { copy(yearlyTurnover = turnover) }
        setSavedState("yearlyTurnover", turnover)
        validateForm()
    }

    private fun clearSavedState() {
        viewModelScope.launch {
            clearAllSavedState()
        }
    }
}