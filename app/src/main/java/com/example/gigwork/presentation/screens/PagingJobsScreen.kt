package com.example.gigwork.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.ImageLoader
import com.example.gigwork.presentation.components.JobSkeletonList
import com.example.gigwork.presentation.components.jobs.JobsTopAppBar
import com.example.gigwork.presentation.components.jobs.JobSearchBar
import com.example.gigwork.presentation.screens.JobFiltersSection
import com.example.gigwork.presentation.components.common.ErrorView
import com.example.gigwork.presentation.components.jobs.JobCard
import com.example.gigwork.presentation.viewmodels.JobsViewModel
import com.example.gigwork.presentation.states.JobsEvent
import com.example.gigwork.presentation.states.JobsFilterData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagingJobsScreen(
    onJobClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
    imageLoader: ImageLoader
) {
    val uiState by viewModel.state.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is JobsEvent.NavigateToJobDetail -> onJobClick(event.jobId)
                is JobsEvent.ShowSnackbar -> {}
                is JobsEvent.ScrollToTop -> {}
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            JobsTopAppBar(
                onFilterClick = { showFilters = !showFilters },
                onProfileClick = onProfileClick,
                showFilters = showFilters
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            JobSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { query -> viewModel.searchJobs(query) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AnimatedVisibility(visible = showFilters) {
                JobFiltersSection(
                    selectedState = uiState.selectedState,
                    selectedDistrict = uiState.selectedDistrict,
                    minSalary = uiState.minSalary,
                    maxSalary = uiState.maxSalary,
                    onApplyFilters = { state, district, minSalary, maxSalary, duration ->
                        viewModel.applyFilters(
                            JobsFilterData(
                                state = state,
                                district = district,
                                minSalary = minSalary,
                                maxSalary = maxSalary
                            )
                        )
                        showFilters = false
                    },
                    onClearFilters = {
                        viewModel.refresh()
                        showFilters = false
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when {
                uiState.isLoading -> {
                    JobSkeletonList(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.errorMessage != null -> {
                    ErrorView(
                        message = uiState.errorMessage?.message ?: "Unknown error occurred",
                        onRetry = viewModel::retry,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            count = uiState.jobs.size,
                            key = { index -> uiState.jobs[index].id }
                        ) { index ->
                            val job = uiState.jobs[index]
                            JobCard(
                                job = job,
                                onClick = { viewModel.navigateToJobDetail(job.id) },
                                imageLoader = imageLoader,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (uiState.isLoadingNextPage) {
                            item { LoadingFooter() }
                        }

                        if (uiState.errorMessage != null && !uiState.isLoading) {
                            item {
                                ErrorFooter(
                                    onRetry = viewModel::retry
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingFooter(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun ErrorFooter(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error loading more items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}