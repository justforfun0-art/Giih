package com.example.gigwork.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.example.gigwork.domain.models.Job
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.components.JobSkeletonList
import com.example.gigwork.presentation.viewmodels.JobsViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun JobsScreen(
    onJobClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val jobs = viewModel.jobsFlow.collectAsLazyPagingItems()
    var showFilters by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JobsEvent.ScrollToTop -> listState.animateScrollToItem(0)
                is JobsEvent.ShowSnackbar -> {} // Handled by error system
            }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isRefreshing,
        onRefresh = viewModel::refresh
    )

    ScreenScaffold(
        errorMessage = state.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Available Jobs") },
                navigationIcon = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.Person, "Profile")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = if (showFilters) Icons.Default.Close else Icons.Default.FilterList,
                            contentDescription = if (showFilters) "Hide Filters" else "Show Filters"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            Column {
                // Search Bar
                JobSearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Filters Section
                AnimatedVisibility(
                    visible = showFilters,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    JobFiltersSection(
                        selectedState = state.selectedState,
                        selectedDistrict = state.selectedDistrict,
                        minSalary = state.minSalary,
                        maxSalary = state.maxSalary,
                        onApplyFilters = viewModel::applyFilters,
                        onClearFilters = viewModel::clearFilters,
                        onStateChanged = viewModel::updateSelectedState,
                        onDistrictChanged = viewModel::updateSelectedDistrict,
                        onSalaryRangeChanged = viewModel::updateSalaryRange
                    )
                }

                when {
                    jobs.loadState.refresh is LoadState.Loading -> {
                        JobSkeletonList(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    jobs.loadState.refresh is LoadState.Error -> {
                        val error = (jobs.loadState.refresh as LoadState.Error).error
                        viewModel.handleLoadError(error)
                        ErrorContent(
                            message = error.message ?: "Failed to load jobs",
                            onRetry = { jobs.retry() }
                        )
                    }
                    jobs.itemCount == 0 -> {
                        EmptyJobsView(
                            showFilters = showFilters,
                            onClearFilters = viewModel::clearFilters
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = jobs,
                                key = { it.id }
                            ) { job ->
                                job?.let {
                                    JobCard(
                                        job = it,
                                        onClick = { onJobClick(it.id) }
                                    )
                                }
                            }

                            item {
                                when (jobs.loadState.append) {
                                    is LoadState.Loading -> LoadingIndicator()
                                    is LoadState.Error -> RetryButton(
                                        onRetry = { jobs.retry() }
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = state.isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )

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
private fun JobSearchBar(
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

@Composable
private fun JobFiltersSection(
    selectedState: String,
    selectedDistrict: String,
    minSalary: Int?,
    maxSalary: Int?,
    onApplyFilters: () -> Unit,
    onClearFilters: () -> Unit,
    onStateChanged: (String) -> Unit,
    onDistrictChanged: (String) -> Unit,
    onSalaryRangeChanged: (Int?, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var minSalaryText by remember(minSalary) { mutableStateOf(minSalary?.toString() ?: "") }
    var maxSalaryText by remember(maxSalary) { mutableStateOf(maxSalary?.toString() ?: "") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location Selection
        LocationSelector(
            selectedState = selectedState,
            selectedDistrict = selectedDistrict,
            onLocationSelected = { state, district ->
                onStateChanged(state)
                onDistrictChanged(district)
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
                            value.toIntOrNull(),
                            maxSalaryText.toIntOrNull()
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
                            minSalaryText.toIntOrNull(),
                            value.toIntOrNull()
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
                Text("Clear Filters")
            }

            Button(
                onClick = onApplyFilters,
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JobCard(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
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
private fun JobStatusChip(
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
private fun EmptyJobsView(
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
        TextButton(
            onClick = onRetry,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error
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