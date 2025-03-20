package com.example.gigwork.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.components.JobSkeletonList
import com.example.gigwork.presentation.viewmodels.EmployerJobsViewModel
import com.example.gigwork.domain.models.Job
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerJobsScreen(
    onNavigateToCreateJob: () -> Unit,
    onJobSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EmployerJobsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        topBar = {
            TopAppBar(
                title = { Text("My Jobs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Filter action
                    IconButton(onClick = { viewModel.toggleFilters() }) {
                        Icon(
                            imageVector = if (uiState.showFilters)
                                Icons.Default.FilterList
                            else
                                Icons.Default.FilterListOff,
                            contentDescription = "Toggle Filters"
                        )
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
            Column {
                // Search Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    placeholder = { Text("Search jobs...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                // Filters Section
                AnimatedVisibility(visible = uiState.showFilters) {
                    JobFiltersSection(
                        selectedState = uiState.selectedState,
                        selectedDistrict = uiState.selectedDistrict,
                        minSalary = uiState.minSalary,
                        maxSalary = uiState.maxSalary,
                        onApplyFilters = { state, district, minSalary, maxSalary, duration ->
                            viewModel.updateFilters(state, district, minSalary, maxSalary, duration)
                        },
                        onClearFilters = viewModel::clearFilters,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                when {
                    uiState.isLoading -> {
                        JobSkeletonList(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    uiState.jobs.isEmpty() -> {
                        EmptyJobsView(
                            onCreateJob = onNavigateToCreateJob
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = uiState.jobs.size,
                                key = { index -> uiState.jobs[index].id }
                            ) { index ->
                                EmployerJobCard(
                                    job = uiState.jobs[index],
                                    onClick = { onJobSelected(uiState.jobs[index].id) },
                                    onStatusChange = { jobId, newStatus ->
                                        viewModel.updateJobStatus(jobId, newStatus)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // FAB for creating new job
            FloatingActionButton(
                onClick = onNavigateToCreateJob,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Job")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployerJobCard(
    job: Job,
    onClick: () -> Unit,
    onStatusChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleLarge
                )
                JobStatusMenu(
                    currentStatus = job.status,
                    onStatusSelected = { newStatus ->
                        onStatusChange(job.id, newStatus)
                    }
                )
            }

            // Salary
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        .format(job.salary)
                        .replace(".00", "") + "/${job.salaryUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${job.location.district}, ${job.location.state}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun JobStatusMenu(
    currentStatus: String,
    onStatusSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(
            onClick = { expanded = true }
        ) {
            Text(currentStatus)
            Icon(Icons.Default.ArrowDropDown, "Change Status")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            listOf("OPEN", "CLOSED", "FILLED", "DRAFT").forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyJobsView(
    onCreateJob: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Work,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No jobs posted yet",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your first job posting to start hiring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCreateJob) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Job")
        }
    }
}