package com.example.gigwork.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.presentation.common.LoadingButton
import com.example.gigwork.presentation.common.LocationSelector
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.CreateJobViewModel
import com.example.gigwork.presentation.states.CreateJobEvent
import com.example.gigwork.presentation.states.JobsEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateJobViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var salaryUnitExpanded by remember { mutableStateOf(false) }
    var workDurationUnitExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CreateJobEvent.NavigateBack -> onNavigateBack()
                is CreateJobEvent.ValidationError -> {
                    // ValidationErrors are now handled through the state
                }
                is CreateJobEvent.CoordinatesUpdated -> {}
                is CreateJobEvent.CostCalculated -> {}
                is CreateJobEvent.DraftDeleted -> {}
                is CreateJobEvent.DraftSaved -> {}
                is CreateJobEvent.JobCreated -> {}
                is CreateJobEvent.LocationSelected -> {}
                is CreateJobEvent.PreviewToggled -> {}
                is CreateJobEvent.ShowSnackbar -> {}
                // Add other events mentioned in the error message
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        topBar = {
            TopAppBar(
                title = { Text("Create Job") },
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
                    .padding(16.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Job Title") },
                    isError = "title" in uiState.validationErrors,
                    supportingText = {
                        uiState.validationErrors["title"]?.let {
                            Text(it.message, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    isError = "description" in uiState.validationErrors,
                    supportingText = {
                        uiState.validationErrors["description"]?.let {
                            Text(it.message, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Salary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.salary,
                        onValueChange = viewModel::updateSalary,
                        label = { Text("Salary") },
                        isError = "salary" in uiState.validationErrors,
                        supportingText = {
                            uiState.validationErrors["salary"]?.let {
                                Text(it.message, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = salaryUnitExpanded,
                        onExpandedChange = { salaryUnitExpanded = !salaryUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.salaryUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Per") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(salaryUnitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = salaryUnitExpanded,
                            onDismissRequest = { salaryUnitExpanded = false }
                        ) {
                            listOf("hourly", "daily", "weekly", "monthly").forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        viewModel.updateSalaryUnit(unit)
                                        salaryUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Work Duration Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.workDuration,
                        onValueChange = viewModel::updateWorkDuration,
                        label = { Text("Duration") },
                        isError = "workDuration" in uiState.validationErrors,
                        supportingText = {
                            uiState.validationErrors["workDuration"]?.let {
                                Text(it.message, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ExposedDropdownMenuBox(
                        expanded = workDurationUnitExpanded,
                        onExpandedChange = { workDurationUnitExpanded = !workDurationUnitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = uiState.workDurationUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(workDurationUnitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = workDurationUnitExpanded,
                            onDismissRequest = { workDurationUnitExpanded = false }
                        ) {
                            listOf("hours", "days", "weeks", "months").forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        viewModel.updateWorkDurationUnit(unit)
                                        workDurationUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location Selection
                LocationSelector(
                    selectedState = uiState.state,
                    selectedDistrict = uiState.district,
                    onLocationSelected = { state, district ->
                        viewModel.updateLocation(state, district)
                    },
                    error = uiState.validationErrors["location"]
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Create Button
                LoadingButton(
                    onClick = viewModel::createJob,
                    text = "Create Job",
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading && uiState.validationErrors.isEmpty(),
                    modifier = Modifier.fillMaxWidth()
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

            // Loading overlay
            if (uiState.isLoading) {
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
    }
}