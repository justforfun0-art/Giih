package com.example.gigwork.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.ImageLoader
import com.example.gigwork.presentation.components.JobSkeletonList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    onJobClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
    imageLoader: ImageLoader
) {
    val jobs = viewModel.jobsFlow.collectAsLazyPagingItems()
    var showFilters by remember { mutableStateOf(false) }

    // Handle initial loading
    val isLoading = jobs.loadState.refresh is LoadState.Loading

    // Handle errors
    val error = when {
        jobs.loadState.refresh is LoadState.Error -> (jobs.loadState.refresh as LoadState.Error).error
        jobs.loadState.append is LoadState.Error -> (jobs.loadState.append as LoadState.Error).error
        else -> null
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
            modifier = Modifier.padding(padding)
        ) {
            // Search Bar
            JobSearchBar(
                query = viewModel.searchQuery.value,
                onQueryChange = viewModel::updateSearchQuery
            )

            // Filters
            AnimatedVisibility(visible = showFilters) {
                JobFiltersSection(
                    selectedState = viewModel.selectedState.value,
                    selectedDistrict = viewModel.selectedDistrict.value,
                    minSalary = viewModel.minSalary.value,
                    maxSalary = viewModel.maxSalary.value,
                    onApplyFilters = viewModel::updateFilters,
                    onClearFilters = viewModel::clearFilters
                )
            }

            when {
                isLoading -> {
                    JobSkeletonList()
                }
                error != null -> {
                    ErrorView(
                        message = error.message ?: "Unknown error occurred",
                        onRetry = { jobs.retry() }
                    )
                }
                else -> {
                    LazyColumn {
                        items(
                            count = jobs.itemCount,
                            key = jobs.itemKey { it.id },
                            contentType = jobs.itemContentType { "job" }
                        ) { index ->
                            val job = jobs[index]
                            if (job != null) {
                                JobCard(
                                    job = job,
                                    onClick = { onJobClick(job.id) },
                                    imageLoader = imageLoader
                                )
                            }
                        }

                        // Add loading footer when appending
                        when (jobs.loadState.append) {
                            is LoadState.Loading -> {
                                item { LoadingFooter() }
                            }
                            is LoadState.Error -> {
                                item {
                                    ErrorFooter(
                                        onRetry = { jobs.retry() }
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ErrorFooter(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Error loading more items",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}