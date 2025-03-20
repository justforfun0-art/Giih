package com.example.gigwork.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.Location
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.presentation.common.LoadingButton
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.LocationViewModel
import com.example.gigwork.presentation.viewmodels.ProfileEvent
import com.example.gigwork.presentation.viewmodels.ProfileState
import com.example.gigwork.presentation.viewmodels.ProfileViewModel
import com.example.gigwork.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    userType: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(userId, userType) {
        viewModel.initializeProfile(userId, userType)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ProfileCreated,
                is ProfileEvent.ProfileUpdated -> onNavigateBack()
                is ProfileEvent.ValidationError -> Unit
            }
        }
    }

    ScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        errorMessage = state.errorMessage,
        onErrorDismiss = viewModel::onErrorDismiss,
        onErrorAction = viewModel::onErrorAction,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (userType == Constants.UserType.EMPLOYEE)
                            "Employee Profile"
                        else
                            "Employer Profile"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        ProfileContent(
            state = state,
            userId = userId,
            userType = userType,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileState,
    userId: String,
    userType: String,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Common fields for both user types
            CommonProfileFields(
                state = state,
                viewModel = viewModel
            )

            // Location selection
            LocationSelector(
                selectedState = state.locationState,
                selectedDistrict = state.locationDistrict,
                onLocationChanged = viewModel::updateLocation,
                error = state.validationErrors["location"]
            )

            // User type specific fields
            if (userType == Constants.UserType.EMPLOYEE) {
                EmployeeProfileFields(
                    state = state,
                    viewModel = viewModel
                )
            } else {
                EmployerProfileFields(
                    state = state,
                    viewModel = viewModel
                )
            }

            SaveProfileButton(
                state = state,
                userId = userId,
                userType = userType,
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

            RequiredFieldsText()
        }

        LoadingOverlay(isVisible = state.isLoading)

        ErrorDialogHandler(
            errorMessage = state.errorMessage,
            onDismiss = viewModel::onErrorDismiss,
            onAction = viewModel::onErrorAction
        )
    }
}

@Composable
private fun CommonProfileFields(
    state: ProfileState,
    viewModel: ProfileViewModel
) {
    ProfileField(
        value = state.name,
        onValueChange = viewModel::updateName,
        label = "Full Name*",
        error = state.validationErrors["name"]
    )

    ProfileField(
        value = state.phone,
        onValueChange = { /* Phone is read-only */ },
        label = "Phone Number",
        error = null,
        readOnly = true
    )

    ProfileField(
        value = state.dateOfBirth,
        onValueChange = viewModel::updateDateOfBirth,
        label = "Date of Birth*",
        error = state.validationErrors["dateOfBirth"]
    )

    ProfileField(
        value = state.gender,
        onValueChange = viewModel::updateGender,
        label = "Gender*",
        error = state.validationErrors["gender"]
    )
}

@Composable
private fun EmployeeProfileFields(
    state: ProfileState,
    viewModel: ProfileViewModel
) {
    ProfileField(
        value = state.qualification,
        onValueChange = viewModel::updateQualification,
        label = "Qualification*",
        error = state.validationErrors["qualification"]
    )

    ProfileField(
        value = state.computerKnowledge,
        onValueChange = viewModel::updateComputerKnowledge,
        label = "Computer Knowledge*",
        error = state.validationErrors["computerKnowledge"],
        minLines = 3
    )
}

@Composable
private fun EmployerProfileFields(
    state: ProfileState,
    viewModel: ProfileViewModel
) {
    ProfileField(
        value = state.companyName ?: "",
        onValueChange = viewModel::updateCompanyName,
        label = "Company Name*",
        error = state.validationErrors["companyName"]
    )

    ProfileField(
        value = state.companyFunction ?: "",
        onValueChange = viewModel::updateCompanyFunction,
        label = "Company Function*",
        error = state.validationErrors["companyFunction"]
    )

    ProfileField(
        value = state.staffCount ?: "",
        onValueChange = viewModel::updateStaffCount,
        label = "Staff Count",
        error = state.validationErrors["staffCount"]
    )

    ProfileField(
        value = state.yearlyTurnover ?: "",
        onValueChange = viewModel::updateYearlyTurnover,
        label = "Yearly Turnover",
        error = state.validationErrors["yearlyTurnover"]
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationSelector(
    selectedState: String,
    selectedDistrict: String,
    onLocationChanged: (state: String, district: String) -> Unit,
    error: String?,
    locationViewModel: LocationViewModel = hiltViewModel()
) {
    val stateData = locationViewModel.state.collectAsState()
    val states = stateData.value.states
    val districts = stateData.value.districts
    var stateExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Location*",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // State dropdown
        ExposedDropdownMenuBox(
            expanded = stateExpanded,
            onExpandedChange = { stateExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedState,
                onValueChange = {},
                readOnly = true,
                label = { Text("State") },
                isError = error != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = stateExpanded,
                onDismissRequest = { stateExpanded = false }
            ) {
                states.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            onLocationChanged(state, "")
                            stateExpanded = false
                            locationViewModel.loadDistricts(state)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // District dropdown
        ExposedDropdownMenuBox(
            expanded = districtExpanded,
            onExpandedChange = { districtExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedDistrict,
                onValueChange = {},
                readOnly = true,
                enabled = selectedState.isNotEmpty(),
                label = { Text("District") },
                isError = error != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = districtExpanded,
                onDismissRequest = { districtExpanded = false }
            ) {
                districts.forEach { district ->
                    DropdownMenuItem(
                        text = { Text(district) },
                        onClick = {
                            onLocationChanged(selectedState, district)
                            districtExpanded = false
                        }
                    )
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun SaveProfileButton(
    state: ProfileState,
    userId: String,
    userType: String,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    LoadingButton(
        onClick = {
            val profile = buildUserProfile(state, userId, userType)
            viewModel.createProfile(userId, profile)
        },
        text = "Save Profile",
        isLoading = state.isLoading,
        enabled = state.validationErrors.isEmpty() && !state.isLoading,
        modifier = modifier
    )
}

private fun buildUserProfile(state: ProfileState, userId: String, userType: String): UserProfile {
    return UserProfile(
        id = "",  // Generated by backend
        userId = userId,
        name = state.name,
        photo = null,
        dateOfBirth = state.dateOfBirth,
        gender = state.gender,
        currentLocation = Location(
            state = state.locationState,
            district = state.locationDistrict,
            latitude = null,
            longitude = null,
            address = null,  // Updated to match Location class
            pinCode = null   // Updated to match Location class
        ),
        preferredLocation = null,
        qualification = if (userType == Constants.UserType.EMPLOYEE) state.qualification else null,
        computerKnowledge = if (userType == Constants.UserType.EMPLOYEE) state.computerKnowledge.toBoolean() else null,
        aadharNumber = null,
        // Employer specific fields
        companyName = if (userType == Constants.UserType.EMPLOYER) state.companyName else null,
        companyFunction = if (userType == Constants.UserType.EMPLOYER) state.companyFunction else null,
        staffCount = if (userType == Constants.UserType.EMPLOYER) state.staffCount?.toIntOrNull() else null,
        yearlyTurnover = if (userType == Constants.UserType.EMPLOYER) state.yearlyTurnover else null
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        readOnly = readOnly,
        supportingText = error?.let {
            { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorSupportingTextColor = MaterialTheme.colorScheme.error,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun LoadingOverlay(isVisible: Boolean) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
private fun ErrorDialogHandler(
    errorMessage: ErrorMessage?,
    onDismiss: () -> Unit,
    onAction: (ErrorAction) -> Unit
) {
    errorMessage?.let { error ->
        if (error.level == ErrorLevel.CRITICAL) {
            ErrorDialog(
                errorMessage = error,
                onDismiss = onDismiss,
                onAction = onAction,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun RequiredFieldsText() {
    Text(
        text = "* Required fields",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}