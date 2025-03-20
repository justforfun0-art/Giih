package com.example.gigwork.core.error.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.gigwork.core.error.model.*

@Composable
fun ErrorDialog(
    errorMessage: ErrorMessage,
    onDismiss: () -> Unit,
    onAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = {
            if (!errorMessage.level.isBlocking()) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !errorMessage.level.isBlocking(),
            dismissOnClickOutside = !errorMessage.level.isBlocking()
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error Icon
                Icon(
                    imageVector = getErrorIcon(errorMessage.level),
                    contentDescription = "Error Icon",
                    tint = getErrorColor(errorMessage.level),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                errorMessage.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Message
                Text(
                    text = errorMessage.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                when (val action = errorMessage.action) {
                    is ErrorAction.Multiple -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { onAction(action.secondary) }
                            ) {
                                Text(getActionLabel(action.secondary))
                            }
                            Button(
                                onClick = { onAction(action.primary) }
                            ) {
                                Text(getActionLabel(action.primary))
                            }
                        }
                    }
                    is ErrorAction.Custom -> {
                        Button(
                            onClick = { onAction(action) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(action.label)
                        }
                    }
                    ErrorAction.Retry -> {
                        Button(
                            onClick = { onAction(ErrorAction.Retry) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry")
                        }
                    }
                    ErrorAction.Dismiss -> {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dismiss")
                        }
                    }
                    ErrorAction.GoBack -> {
                        Button(
                            onClick = { onAction(ErrorAction.GoBack) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go Back")
                        }
                    }
                    null -> {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getErrorColor(level: ErrorLevel): androidx.compose.ui.graphics.Color {
    return when (level) {
        ErrorLevel.CRITICAL -> MaterialTheme.colorScheme.error
        ErrorLevel.ERROR -> MaterialTheme.colorScheme.error
        ErrorLevel.WARNING -> MaterialTheme.colorScheme.secondary
        ErrorLevel.INFO -> MaterialTheme.colorScheme.primary
    }
}

private fun getErrorIcon(level: ErrorLevel): ImageVector = when (level) {
    ErrorLevel.CRITICAL -> Icons.Default.ErrorOutline
    ErrorLevel.ERROR -> Icons.Default.Error
    ErrorLevel.WARNING -> Icons.Default.Warning
    ErrorLevel.INFO -> Icons.Default.Info
}

private fun getActionLabel(action: ErrorAction): String = when (action) {
    is ErrorAction.Retry -> "Retry"
    is ErrorAction.Dismiss -> "Dismiss"
    is ErrorAction.Custom -> action.label
    is ErrorAction.Multiple -> getActionLabel(action.primary)
    ErrorAction.GoBack -> "Go Back"
}