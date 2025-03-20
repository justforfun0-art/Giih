package com.example.gigwork.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.DashboardMetrics
import com.example.gigwork.presentation.components.AnalyticsCard
import com.example.gigwork.presentation.components.RecentJobPostingsSection
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.EmployerDashboardEvent
import com.example.gigwork.presentation.viewmodels.EmployerDashboardViewModel
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployerDashboardScreen(
    onNavigateToCreateJob: () -> Unit,
    onNavigateToJobs: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: EmployerDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EmployerDashboardEvent.NavigateToJobs -> onNavigateToJobs()
                is EmployerDashboardEvent.NavigateToProfile -> onNavigateToProfile(event.userId)
                is EmployerDashboardEvent.NavigateToCreateJob -> onNavigateToCreateJob()
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = viewModel::clearError,
        onErrorAction = viewModel::handleErrorAction,
        showNavigationBar = true,
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.navigateToProfile() }) {
                        Icon(Icons.Default.Person, "Profile")
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
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    DashboardContent(
                        metrics = uiState.metrics,
                        onCreateJobClick = viewModel::navigateToCreateJob,
                        onViewAllJobsClick = viewModel::navigateToJobs
                    )
                }
            }

            // Create job FAB
            FloatingActionButton(
                onClick = viewModel::navigateToCreateJob,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Create Job")
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
private fun DashboardContent(
    metrics: DashboardMetrics?,
    onCreateJobClick: () -> Unit,
    onViewAllJobsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        WelcomeSection("Employer")

        if (metrics != null) {
            MetricsSection(metrics)

            RecentJobPostingsSection(
                jobs = metrics.recentJobs,
                onViewAllClick = onViewAllJobsClick
            )

            if (metrics.recentJobs.isEmpty()) {
                EmptyJobsSection(onCreateJobClick)
            }
        } else {
            EmptyDashboardSection(onCreateJobClick)
        }

        // Add extra space for the FAB
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun WelcomeSection(companyName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = companyName.ifEmpty { "Employer" },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Here's an overview of your recruiting activity",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricsSection(metrics: DashboardMetrics) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Key Metrics",
            style = MaterialTheme.typography.titleLarge
        )

        // Pre-compute the metrics items in a composable context
        val metricItems = listOf(
            MetricItem(
                icon = Icons.Default.WorkOutline,
                title = "Active Jobs",
                value = metrics.openJobsCount.toString(),
                backgroundColor = MaterialTheme.colorScheme.primaryContainer
            ),
            MetricItem(
                icon = Icons.Default.PeopleOutline,
                title = "Applications",
                value = metrics.totalApplicantsCount.toString(),
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            MetricItem(
                icon = Icons.Default.AccessTime,
                title = "Pending",
                value = metrics.activeApplicantsCount.toString(),
                backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            MetricItem(
                icon = Icons.Default.Check,
                title = "Hired",
                value = metrics.hiredCount.toString(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(180.dp)
        ) {
            // Now using the pre-computed list
            items(metricItems) { item ->
                AnalyticsCard(
                    icon = item.icon,
                    title = item.title,
                    value = item.value,
                    backgroundColor = item.backgroundColor
                )
            }
        }
    }
}
@Composable
private fun EmptyJobsSection(onCreateJobClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Work,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "No job postings yet",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Create your first job posting to start finding candidates",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onCreateJobClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Job")
            }
        }
    }
}

@Composable
private fun EmptyDashboardSection(onCreateJobClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Dashboard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Welcome to your Dashboard",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Start by creating a job posting to see your metrics and activity",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = onCreateJobClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Job")
        }
    }
}

private data class MetricItem(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val backgroundColor: Color
)

@Composable
private fun getMetricsItems(metrics: DashboardMetrics): List<MetricItem> = listOf(
    MetricItem(
        icon = Icons.Default.WorkOutline,
        title = "Active Jobs",
        value = metrics.openJobsCount.toString(), // Using existing property
        backgroundColor = MaterialTheme.colorScheme.primaryContainer
    ),
    MetricItem(
        icon = Icons.Default.PeopleOutline,
        title = "Applications",
        value = metrics.totalApplicantsCount.toString(), // Using existing property
        backgroundColor = MaterialTheme.colorScheme.secondaryContainer
    ),
    MetricItem(
        icon = Icons.Default.AccessTime,
        title = "Pending",
        value = metrics.activeApplicantsCount.toString(), // Using existing property
        backgroundColor = MaterialTheme.colorScheme.tertiaryContainer
    ),
    MetricItem(
        icon = Icons.Default.Check,
        title = "Hired",
        value = metrics.hiredCount.toString(), // Using existing property
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    )
)