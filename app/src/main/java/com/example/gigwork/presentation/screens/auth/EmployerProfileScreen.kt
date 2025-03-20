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
import com.example.gigwork.presentation.components.EmployerStatisticsSection
import com.example.gigwork.presentation.components.ProfilePhotoSection
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.EmployerProfileViewModel
import com.example.gigwork.presentation.viewmodels.EmployerProfileEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: EmployerProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmployerProfileEvent.NavigateToSettings -> onNavigateToSettings()
                is EmployerProfileEvent.Logout -> onLogout()
                is EmployerProfileEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        topBar = {
            TopAppBar(
                title = { Text("Company Profile") },
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
                EmployerProfileContent(
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
private fun EmployerProfileContent(
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
            text = "Company profile not found",
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
        // Company Header with Logo
        ProfilePhotoSection(
            photoUrl = user.profile?.photo,
            name = user.profile?.companyName ?: user.name,
            isEditable = isEditMode,
            onPhotoChanged = { /* Handle photo change */ }
        )

        Divider()

        // Employer Statistics (Active Jobs, Applications, etc.)
        EmployerStatisticsSection(
            activeJobs = 0, // Replace with actual data
            applications = 0,
            hired = 0
        )

        Divider()

        // Company Information
        CompanyInformation(
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
private fun CompanyInformation(
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
                text = "Company Information",
                style = MaterialTheme.typography.titleMedium
            )

            // Basic Information
            ProfileField(
                label = "Company Name",
                value = user.profile?.companyName ?: "",
                isEditable = isEditMode,
                onValueChange = { onFieldChanged("companyName", it) },
                error = validationErrors["companyName"],
                icon = Icons.Default.Business
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

            // Company-specific fields
            user.profile?.let { profile ->
                ProfileField(
                    label = "Company Function",
                    value = profile.companyFunction ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("companyFunction", it) },
                    error = validationErrors["companyFunction"],
                    icon = Icons.Default.Category
                )

                ProfileField(
                    label = "Staff Count",
                    value = profile.staffCount?.toString() ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("staffCount", it) },
                    error = validationErrors["staffCount"],
                    icon = Icons.Default.People
                )

                ProfileField(
                    label = "Yearly Turnover",
                    value = profile.yearlyTurnover ?: "",
                    isEditable = isEditMode,
                    onValueChange = { onFieldChanged("yearlyTurnover", it) },
                    error = validationErrors["yearlyTurnover"],
                    icon = Icons.Default.AttachMoney
                )

                // Location information
                profile.currentLocation?.let { location ->
                    Text(
                        text = "Location",
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