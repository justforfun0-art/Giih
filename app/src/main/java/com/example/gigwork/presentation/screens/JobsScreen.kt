package com.example.gigwork.presentation.screens

import com.example.gigwork.presentation.common.LocationSelector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.domain.models.Job
import com.example.gigwork.presentation.components.JobSkeletonList
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.states.JobsEvent
import com.example.gigwork.presentation.viewmodels.JobsViewModel
import java.text.NumberFormat
import java.util.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import com.example.gigwork.presentation.states.JobsState
import androidx.compose.animation.AnimatedVisibility
import coil.ImageLoader
import com.example.gigwork.presentation.components.jobs.JobCard
import com.example.gigwork.presentation.states.JobsFilterData



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    userId: String, // Add this parameter
    onJobClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    imageLoader: ImageLoader,
    viewModel: JobsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState(initial = JobsState())
    var showFilters by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JobsEvent.NavigateToJobDetail -> onJobClick(event.jobId)
                is JobsEvent.ShowSnackbar -> {} // Handled by error system
                is JobsEvent.ScrollToTop -> listState.animateScrollToItem(0)
                else -> {}
            }
        }
    }

    ScreenScaffold(
        errorMessage = state.errorMessage,
        onErrorDismiss = { viewModel.onEvent(JobsEvent.DismissError) },
        onErrorAction = { action ->
            viewModel.onEvent(JobsEvent.HandleError(action))
        },
        showNavigationBar = true,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ){ padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column {
                // Search Bar
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { query ->
                        viewModel.onEvent(JobsEvent.SearchQueryChanged(query))
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Filters Section
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    JobFilters(
                        selectedState = state.selectedState ?: "",
                        selectedDistrict = state.selectedDistrict ?: "",
                        minSalary = state.minSalary,
                        maxSalary = state.maxSalary,
                        onApplyFilters = { state, district, min, max ->
                            viewModel.applyFilters(
                                JobsFilterData(
                                    state = state,
                                    district = district,
                                    minSalary = min,
                                    maxSalary = max
                                )
                            )
                            showFilters = false
                        },
                        onClearFilters = {
                            viewModel.refresh()
                            showFilters = false
                        },
                        onLocationSelected = { selectedState, selectedDistrict ->
                            viewModel.applyFilters(
                                JobsFilterData(
                                    state = selectedState,
                                    district = selectedDistrict,
                                    minSalary = state.minSalary,
                                    maxSalary = state.maxSalary
                                )
                            )
                        },
                        onSalaryRangeChanged = { min, max ->
                            viewModel.applyFilters(
                                JobsFilterData(
                                    state = state.selectedState,
                                    district = state.selectedDistrict,
                                    minSalary = min,
                                    maxSalary = max
                                )
                            )
                        },
                        imageLoader = imageLoader
                    )
                }

                when {
                    state.isLoading -> {
                        JobSkeletonList(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    state.errorMessage != null -> {
                        ErrorContent(
                            message = state.errorMessage?.message ?: "Failed to load jobs",
                            onRetry = { viewModel.onEvent(JobsEvent.Refresh) }
                        )
                    }
                    state.jobs.isEmpty() -> {
                        EmptyJobsView(
                            showFilters = showFilters,
                            onClearFilters = { viewModel.onEvent(JobsEvent.Refresh) }
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = state.jobs.size,
                                key = { index -> state.jobs[index].id }
                            ) { index ->
                                JobCard(
                                    job = state.jobs[index],
                                    onClick = { onJobClick(state.jobs[index].id) },
                                    imageLoader = imageLoader,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (state.isLoadingNextPage) {
                                item { LoadingIndicator() }
                            }
                        }
                    }
                }
            }

            // FAB for refresh
            if (!showFilters) {
                FloatingActionButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
 fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search jobs...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobFilters(
    selectedState: String,
    selectedDistrict: String,
    minSalary: Double?,
    maxSalary: Double?,
    onApplyFilters: (String, String, Double?, Double?) -> Unit,
    onClearFilters: () -> Unit,
    onLocationSelected: (String, String) -> Unit,
    onSalaryRangeChanged: (Double?, Double?) -> Unit,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    var minSalaryText by remember(minSalary) { mutableStateOf(minSalary?.toString() ?: "") }
    var maxSalaryText by remember(maxSalary) { mutableStateOf(maxSalary?.toString() ?: "") }
    var currentState by remember { mutableStateOf(selectedState) }
    var currentDistrict by remember { mutableStateOf(selectedDistrict) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Selection
        LocationSelector(
            onLocationSelected = { state, district ->
                currentState = state
                currentDistrict = district
                onLocationSelected(state, district)
            }
        )

        // Salary Range
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Salary Range",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = minSalaryText,
                    onValueChange = { value ->
                        minSalaryText = value
                        onSalaryRangeChanged(
                            value.toDoubleOrNull(),
                            maxSalaryText.toDoubleOrNull()
                        )
                    },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    prefix = { Text("₹") }
                )

                OutlinedTextField(
                    value = maxSalaryText,
                    onValueChange = { value ->
                        maxSalaryText = value
                        onSalaryRangeChanged(
                            minSalaryText.toDoubleOrNull(),
                            value.toDoubleOrNull()
                        )
                    },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    prefix = { Text("₹") }
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear")
            }

            Button(
                onClick = {
                    onApplyFilters(
                        currentState,
                        currentDistrict,
                        minSalaryText.toDoubleOrNull(),
                        maxSalaryText.toDoubleOrNull()
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCard(
    job: Job,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = {},
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val formattedSalary = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                .format(job.salary.toDouble())
                .replace(".00", "")

            Text(
                text = "$formattedSalary/${job.salaryUnit}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Text(
                    text = "${job.workDuration} ${job.workDurationUnit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            JobStatusChip(status = job.status)
        }
    }
}

@Composable
fun JobStatusChip(
    status: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when (status.uppercase()) {
            "OPEN" -> MaterialTheme.colorScheme.primaryContainer
            "CLOSED" -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = when (status.uppercase()) {
            "OPEN" -> MaterialTheme.colorScheme.onPrimaryContainer
            "CLOSED" -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(4.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
 fun EmptyJobsView(
    showFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.WorkOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No jobs found",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (showFilters) {
                "Try adjusting your filters"
            } else {
                "Try a different search"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (showFilters) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun RetryButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}