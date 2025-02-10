package com.example.gigwork.core.error.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                    contentDescription = null,
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
                                Text(action.secondary.getActionLabel())
                            }
                            Button(
                                onClick = { onAction(action.primary) }
                            ) {
                                Text(action.primary.getActionLabel())
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
                    is ErrorAction.Retry -> {
                        Button(
                            onClick = { onAction(action) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry")
                        }
                    }
                    is ErrorAction.Dismiss -> {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("OK")
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

                    ErrorAction.GoBack -> TODO()
                }
            }
        }
    }
}

@Composable
private fun getErrorColor(level: ErrorLevel) = when (level) {
    ErrorLevel.CRITICAL -> MaterialTheme.colorScheme.error
    ErrorLevel.ERROR -> MaterialTheme.colorScheme.error
    ErrorLevel.WARNING -> MaterialTheme.colorScheme.secondary
    ErrorLevel.INFO -> MaterialTheme.colorScheme.primary
}

private fun getErrorIcon(level: ErrorLevel): ImageVector = when (level) {
    ErrorLevel.CRITICAL -> Icons.Default.ErrorOutline
    ErrorLevel.ERROR -> Icons.Default.Error
    ErrorLevel.WARNING -> Icons.Default.Warning
    ErrorLevel.INFO -> Icons.Default.Info
}

private fun ErrorAction.getActionLabel(): String = when (this) {
    is ErrorAction.Retry -> "Retry"
    is ErrorAction.Dismiss -> "Dismiss"
    is ErrorAction.Custom -> this.label
    is ErrorAction.Multiple -> this.primary.getActionLabel()
    else -> "OK"
}