package com.example.gigwork.presentation.screens.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gigwork.core.error.model.ErrorMessage
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.ui.ErrorSnackbar

// Base UiState interface that all screen states should implement
interface UiState {
    val isLoading: Boolean
    val errorMessage: ErrorMessage?
}

// Common screen state that includes loading and error states
data class BaseScreenState(
    override val isLoading: Boolean = false,
    override val errorMessage: ErrorMessage? = null
) : UiState

// Screen scaffold that includes error handling
@Composable
fun ScreenScaffold(
    errorMessage: ErrorMessage?,
    onErrorDismiss: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        snackbarHost = {
            Box(modifier = Modifier.padding(bottom = 16.dp)) {
                errorMessage?.let { message ->
                    ErrorSnackbar(
                        errorMessage = message,
                        onDismiss = onErrorDismiss,
                        onAction = onErrorAction,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    ) { paddingValues ->
        content(paddingValues)
    }
}

// Loading indicator component
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

// Error content component for full screen errors
@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}