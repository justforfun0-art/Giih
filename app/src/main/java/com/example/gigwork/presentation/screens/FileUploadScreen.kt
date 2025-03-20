package com.example.gigwork.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.core.error.ui.ErrorSnackbar
import com.example.gigwork.presentation.viewmodels.FileUploadViewModel
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.presentation.viewmodels.FileUploadEvent
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileUploadScreen(
    onNavigateBack: () -> Unit,
    onPreviewFile: (String) -> Unit,
    viewModel: FileUploadViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // File picker launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectFile(it) }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is FileUploadEvent.ShowFilePicker -> {
                    launcher.launch("*/*")
                }
                is FileUploadEvent.NavigateToPreview -> {
                    onPreviewFile(event.fileUrl)
                }
                is FileUploadEvent.UploadComplete -> {
                    scope.launch {
                        snackbarHostState.showSnackbar("File uploaded successfully!")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload File") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // File Selection Area
                FileSelectionArea(
                    selectedUri = uiState.selectedFileUri,
                    fileName = uiState.fileName,
                    fileSize = uiState.fileSize,
                    onSelectFile = { viewModel.showFilePicker() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Upload Progress
                if (uiState.isLoading || uiState.progress > 0) {
                    UploadProgress(
                        progress = uiState.progress,
                        isLoading = uiState.isLoading
                    )
                }

                // Upload Button
                if (uiState.selectedFileUri != null && !uiState.isLoading) {
                    Button(
                        onClick = { viewModel.uploadFile() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload File")
                    }
                }

                // Preview Button
                if (uiState.uploadedFileUrl != null) {
                    OutlinedButton(
                        onClick = { viewModel.previewFile() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Preview File")
                    }
                }
            }

            // Error Handling
            uiState.errorMessage?.let { error ->
                when (error.level) {
                    ErrorLevel.CRITICAL -> {
                        ErrorDialog(
                            errorMessage = error,
                            onDismiss = viewModel::clearError,
                            onAction = viewModel::handleErrorAction
                        )
                    }
                    else -> {
                        ErrorSnackbar(
                            errorMessage = error,
                            onDismiss = viewModel::clearError,
                            onAction = viewModel::handleErrorAction,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSelectionArea(
    selectedUri: Uri?,
    fileName: String?,
    fileSize: Long?,
    onSelectFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(MaterialTheme.shapes.large)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.large
            )
            .clickable { onSelectFile() },
        color = MaterialTheme.colorScheme.surface
    ) {
        if (selectedUri != null) {
            // Show selected file preview
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedUri.toString().endsWith(".pdf", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    AsyncImage(
                        model = selectedUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                fileName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                fileSize?.let {
                    Text(
                        text = formatFileSize(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Show upload prompt
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Click to select a file",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Maximum size: 5MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun UploadProgress(
    progress: Float,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = progress,  // Changed from { progress } to progress
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoading && progress == 0f) {
                "Preparing upload..."
            } else {
                "Uploading: ${(progress * 100).toInt()}%"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
    }
}