package com.example.gigwork.presentation.screens

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
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.presentation.components.LoadingButton
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.ProfileEvent
import com.example.gigwork.presentation.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Handle profile loading on init
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ProfileCreated,
                is ProfileEvent.ProfileUpdated -> onNavigateBack()
                is ProfileEvent.ValidationError -> {
                    // Validation errors are handled through state
                }
            }
        }
    }

    ScreenScaffold(
        errorMessage = state.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

                ProfileField(
                    value = state.currentLocation,
                    onValueChange = viewModel::updateLocation,
                    label = "Current Location*",
                    error = state.validationErrors["currentLocation"]
                )

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

                LoadingButton(
                    onClick = {
                        val profile = UserProfile(
                            userId = userId,
                            dateOfBirth = state.dateOfBirth,
                            gender = state.gender,
                            currentLocation = state.currentLocation,
                            qualification = state.qualification,
                            computerKnowledge = state.computerKnowledge
                        )
                        viewModel.createProfile(userId, profile)
                    },
                    text = "Save Profile",
                    isLoading = state.isLoading,
                    enabled = state.validationErrors.isEmpty() && !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )

                Text(
                    text = "* Required fields",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Show loading overlay when loading
            if (state.isLoading) {
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

            // Show critical errors in dialog
            state.errorMessage?.let { error ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
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