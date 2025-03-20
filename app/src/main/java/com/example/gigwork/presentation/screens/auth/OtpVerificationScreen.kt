package com.example.gigwork.presentation.screens.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.viewmodels.AuthEvent
import com.example.gigwork.presentation.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun OtpVerificationScreen(
    navController: NavController,
    phoneNumber: String,
    verificationId: String = "",
    userType: String = "",
    onVerificationComplete: (userId: String, userType: String) -> Unit = { _, _ -> },
    onBackPressed: () -> Unit = { navController.popBackStack() },
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Save verification data when the screen is displayed
    LaunchedEffect(verificationId, userType) {
        if (verificationId.isNotEmpty()) {
            viewModel.saveVerificationData(verificationId, userType)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    var otpValue by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    // Countdown timer for resend
    LaunchedEffect(key1 = Unit) {
        while (timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        canResend = true
    }

    // Handle auth events
    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToJobsScreen -> {
                    navController.navigate(Screen.Jobs.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
                is AuthEvent.NavigateToEmployerDashboard -> {
                    navController.navigate(Screen.EmployerDashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
                is AuthEvent.NavigateToProfileSetup -> {
                    navController.navigate(
                        Screen.ProfileSetup.createRoute(
                            event.userId,
                            viewModel.getUserType()
                        )
                    ) {
                        popUpTo(Screen.OtpVerification.route) { inclusive = true }
                    }
                }
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Verification Code",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter the 6-digit code sent to $phoneNumber",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OtpTextField(
            otpText = otpValue,
            onOtpTextChange = { value, _ -> otpValue = value }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.verifyCode(otpValue) },
            enabled = otpValue.length == 6 && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify Code")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (canResend) {
            TextButton(
                onClick = {
                    // Check if context is a ComponentActivity
                    val activity = context as? ComponentActivity
                    viewModel.resendVerificationCode(
                        activity = activity
                    )
                    timeRemaining = 60
                    canResend = false
                }
            ) {
                Text("Resend Code")
            }
        } else {
            Text(
                text = "Resend code in ${timeRemaining}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBackPressed
        ) {
            Text("Change Phone Number")
        }

        if (uiState.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun OtpTextField(
    modifier: Modifier = Modifier,
    otpText: String,
    otpCount: Int = 6,
    onOtpTextChange: (String, Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        if (otpText.length > otpCount) {
            onOtpTextChange(otpText.substring(0, otpCount), true)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(otpCount) { index ->
            OtpDigitTextField(
                index = index,
                text = otpText
            ) {
                onOtpTextChange(otpText + it, otpText.length == otpCount - 1)
            }

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OtpDigitTextField(
    index: Int,
    text: String,
    onValueChange: (String) -> Unit
) {
    val isFocused = text.length == index
    val value = when {
        index >= text.length -> ""
        else -> text[index].toString()
    }

    OutlinedTextField(
        value = value,
        onValueChange = {
            if (it.length <= 1) {
                onValueChange(it)
            }
        },
        modifier = Modifier
            .width(46.dp)
            .aspectRatio(1f),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Next
        ),
        singleLine = true,
        maxLines = 1
    )
}