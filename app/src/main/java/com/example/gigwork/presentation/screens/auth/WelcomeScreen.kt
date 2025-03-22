// presentation/screens/auth/WelcomeScreen.kt
package com.example.gigwork.presentation.screens.auth

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.gigwork.R
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.AuthEvent
import com.example.gigwork.presentation.viewmodels.AuthViewModel
import com.example.gigwork.util.Constants
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun WelcomeScreen(
    onNavigateToEmployeeAuth: () -> Unit,
    onNavigateToEmployerAuth: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Lifecycle logging
    DisposableEffect(Unit) {
        Log.d("WelcomeScreen", "Welcome screen mounted")
        onDispose {
            Log.d("WelcomeScreen", "Welcome screen disposed")
        }
    }

    // Track navigation status to prevent multiple events
    val hasNavigated = remember { mutableStateOf(false) }

    // Use the lifecycle owner for proper event collection
    val lifecycleOwner = LocalLifecycleOwner.current

    // Collect navigation events with proper lifecycle handling
    LaunchedEffect(viewModel, lifecycleOwner) {
        viewModel.events
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .distinctUntilChanged()
            .collect { event ->
                when (event) {
                    is AuthEvent.NavigateToPhoneEntry -> {
                        if (!hasNavigated.value) {
                            hasNavigated.value = true
                            when (event.userType) {
                                Constants.UserType.EMPLOYEE -> onNavigateToEmployeeAuth()
                                Constants.UserType.EMPLOYER -> onNavigateToEmployerAuth()
                                else -> Log.w("WelcomeScreen", "Unknown user type: ${event.userType}")
                            }
                        }
                    }
                    else -> { /* Ignore other events */ }
                }
            }
    }

    // Stable UI measurements
    val buttonHeight = 56.dp
    val logoSize = 120.dp
    val spacingHeight = 24.dp

    ScreenScaffold(
        errorMessage = null,
        onErrorDismiss = { },
        onErrorAction = { },
        showNavigationBar = false,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // App Logo - optimized with fixed size
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "GigWork Logo",
                modifier = Modifier
                    .size(logoSize)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(spacingHeight))

            // App Title
            Text(
                text = "GigWork",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // App Tagline
            Text(
                text = "Connect with opportunities",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            // Description Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your marketplace for local jobs",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Whether you're looking for work or hiring, GigWork helps you connect with opportunities in your area.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Job Seeker Button
            Button(
                onClick = {
                    // Only trigger the user type selection
                    if (!hasNavigated.value) {
                        viewModel.setUserType(Constants.UserType.EMPLOYEE)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Find Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Employer Button
            OutlinedButton(
                onClick = {
                    // Only trigger the user type selection
                    if (!hasNavigated.value) {
                        viewModel.setUserType(Constants.UserType.EMPLOYER)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(buttonHeight),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Post Jobs",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}