package com.example.gigwork.core.error.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gigwork.core.error.model.ErrorAction
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.model.ErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Extension function for getting action label
fun ErrorAction.getActionLabel(): String = when (this) {
    is ErrorAction.Retry -> "RETRY"
    is ErrorAction.Dismiss -> "DISMISS"
    is ErrorAction.Custom -> label.uppercase()
    is ErrorAction.Multiple -> primary.getActionLabel()
    ErrorAction.GoBack -> TODO()
}

// Extension function for auto-hide behavior
fun ErrorLevel.shouldAutoHide(): Boolean = when (this) {
    ErrorLevel.INFO, ErrorLevel.WARNING -> true
    ErrorLevel.ERROR, ErrorLevel.CRITICAL -> false
}

// Extension function for auto-hide duration
fun ErrorLevel.getAutoHideDuration(): Long = when (this) {
    ErrorLevel.INFO -> 3000L // 3 seconds
    ErrorLevel.WARNING -> 5000L // 5 seconds
    ErrorLevel.ERROR -> 0L // Don't auto-hide
    ErrorLevel.CRITICAL -> 0L // Don't auto-hide
}

@Composable
fun ErrorSnackbar(
    errorMessage: ErrorMessage,
    onDismiss: () -> Unit,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val scope = rememberCoroutineScope()
    val shouldAutoHide = errorMessage.level.shouldAutoHide()
    val duration = errorMessage.level.getAutoHideDuration()

    // Auto-hide effect
    LaunchedEffect(errorMessage) {
        if (shouldAutoHide && duration > 0) {
            delay(duration)
            onDismiss()
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) {
        Surface(
            color = getSnackbarColor(errorMessage.level),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    if (errorMessage.title != null) {
                        Text(
                            text = errorMessage.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = errorMessage.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    when (val action = errorMessage.action) {
                        is ErrorAction.Multiple -> {
                            action.secondary?.let { secondary ->
                                TextButton(
                                    onClick = { onAction(secondary) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(secondary.getActionLabel())
                                }
                            }
                            TextButton(
                                onClick = { onAction(action.primary) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(action.primary.getActionLabel())
                            }
                        }
                        is ErrorAction.Custom -> {
                            TextButton(
                                onClick = { onAction(action) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(action.getActionLabel())
                            }
                        }
                        is ErrorAction.Retry -> {
                            TextButton(
                                onClick = { onAction(action) },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(action.getActionLabel())
                            }
                        }
                        is ErrorAction.Dismiss -> {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        null -> {
                            // Show dismiss icon only if not auto-hiding
                            if (!shouldAutoHide) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        ErrorAction.GoBack -> TODO()
                    }
                }
            }
        }
    }
}

@Composable
private fun getSnackbarColor(level: ErrorLevel) = when (level) {
    ErrorLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    ErrorLevel.ERROR -> MaterialTheme.colorScheme.error
    ErrorLevel.WARNING -> MaterialTheme.colorScheme.secondaryContainer
    ErrorLevel.INFO -> MaterialTheme.colorScheme.surfaceVariant
}

// Preview/Example
@Composable
fun ErrorSnackbarExample() {
    var errorMessage by remember { mutableStateOf<ErrorMessage?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Your screen content

        // Show error snackbar when there's an error message
        errorMessage?.let { message ->
            ErrorSnackbar(
                errorMessage = message,
                onDismiss = {
                    errorMessage = null
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                    }
                },
                onAction = { action ->
                    when (action) {
                        is ErrorAction.Retry -> {
                            // Handle retry
                        }
                        is ErrorAction.Dismiss -> {
                            errorMessage = null
                        }
                        is ErrorAction.Custom -> {
                            // Handle custom action
                        }
                        is ErrorAction.Multiple -> {
                            // Handle multiple actions
                        }

                        ErrorAction.GoBack -> TODO()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbarHostState = snackbarHostState
            )
        }
    }
}