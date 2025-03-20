package com.example.gigwork.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.User
import com.example.gigwork.presentation.common.LoadingButton
import com.example.gigwork.presentation.components.UserStatisticsSection
import com.example.gigwork.presentation.components.ProfilePhotoSection
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.EmployeeProfileViewModel
import com.example.gigwork.presentation.viewmodels.EmployeeProfileEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: EmployeeProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmployeeProfileEvent.NavigateToSettings -> onNavigateToSettings()
                is EmployeeProfileEvent.Logout -> onLogout()
                is EmployeeProfileEvent.NavigateBack -> onNavigateBack()
                else -> {} // Handle other events if necessary
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                    IconButton(onClick = viewModel::onLogoutClick) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ProfileContent(
                    user = uiState.user,
                    isEditMode = uiState.isEditMode,
                    onEditClick = viewModel::toggleEditMode,
                    onSaveClick = viewModel::saveProfile,
                    isSaving = uiState.isSaving,
                    onFieldChanged = viewModel::updateProfileField,
                    validationErrors = uiState.validationErrors
                )
            }

            // Critical Error Dialog
            uiState.errorMessage?.let { error ->
                if (error.level == ErrorLevel.CRITICAL) {
                    ErrorDialog(
                        errorMessage = error,
                        onDismiss = viewModel::clearError,
                        onAction = viewModel::handleErrorAction,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    user: User?,
    isEditMode: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    isSaving: Boolean,
    onFieldChanged: (String, String) -> Unit,
    validationErrors: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (user == null) {
        Text(
            text = "User not found",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .wrapContentSize(Alignment.Center)
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile Header with Photo
        ProfilePhotoSection(
            photoUrl = user.profile?.photo,
            name = user.name,
            isEditable = isEditMode,
            onPhotoChanged = { /* Handle photo change */ }
        )

        Divider()

        // User Statistics (Applied Jobs, Interviews, etc.)
        UserStatisticsSection(
            appliedJobs = 0, // Replace with actual data
            interviews = 0,
            offers = 0
        )

        Divider()

        // Profile Information
        ProfileInformation(
            user = user,
            isEditMode = isEditMode,
            onFieldChanged = onFieldChanged,
            validationErrors = validationErrors
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Edit/Save Button
        if (isEditMode) {
            LoadingButton(
                onClick = onSaveClick,
                text = "Save Profile",
                isLoading = isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileInformation(
    user: User,
    isEditMode: Boolean,
    onFieldChanged: (String, String) -> Unit,
    validationErrors: Map<String, String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium
            )

            // Profile Fields
            ProfileField(
                label = "Name",
                value = user.name,
                isEditable = isEditMode,
                onValueChange = { onFieldChanged("name", it) },
                error = validationErrors["name"],
                icon = Icons.Default.Person
            )

            ProfileField(
                label = "Phone",
                value = user.phone ?: "",
                isEditable = isEditMode,
                onValueChange = { onFieldChanged("phone", it) },
                error = validationErrors["phone"],
                icon = Icons.Default.Phone
            )

            ProfileField(
                label = "Email",
                value = user.email ?: "",
                isEditable = isEditMode,
                onValueChange = { onFieldChanged("email", it) },
                error = validationErrors["email"],
                icon = Icons.Default.Email
            )

            // Profile-specific fields
            user.profile?.let { profile ->
                ProfileField(
                    label = "Date of Birth",
                    value = profile.dateOfBirth ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("dateOfBirth", it) },
                    error = validationErrors["dateOfBirth"],
                    icon = Icons.Default.CalendarToday
                )

                ProfileField(
                    label = "Gender",
                    value = profile.gender ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("gender", it) },
                    error = validationErrors["gender"],
                    icon = Icons.Default.Person
                )

                ProfileField(
                    label = "Qualification",
                    value = profile.qualification ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("qualification", it) },
                    error = validationErrors["qualification"],
                    icon = Icons.Default.School
                )

                // Location information
                profile.currentLocation?.let { location ->
                    Text(
                        text = "Current Location",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${location.district}, ${location.state}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit,
    error: String?,
    icon: ImageVector
) {
    if (isEditable) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null) },
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value.ifEmpty { "Not provided" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}