package com.example.gigwork.presentation.screens.auth

import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.viewmodels.AuthEvent
import com.example.gigwork.presentation.viewmodels.AuthViewModel

// Utility function to get activity in Compose
@Composable
fun rememberActivity(): ComponentActivity? {
    val context = LocalContext.current
    return remember(context) {
        context as? ComponentActivity
    }
}

@Composable
fun PhoneNumberEntryScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current as ComponentActivity
    var phoneNumber by remember { mutableStateOf("") }
    var isPhoneValid by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.CodeSent -> {
                    // Get verification ID from ViewModel state
                    val verificationId = viewModel.uiState.value.verificationId ?: ""
                    // Get user type from ViewModel
                    val userType = viewModel.getUserType()

                    navController.navigate(
                        Screen.OtpVerification.createRoute(
                            phoneNumber = phoneNumber,
                            verificationId = verificationId,
                            userType = userType
                        )
                    )
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
            text = "Enter Your Phone Number",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We'll send you a verification code",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue ->
                phoneNumber = newValue
                isPhoneValid = newValue.isEmpty() || Patterns.PHONE.matcher(newValue).matches()
            },
            label = { Text("Phone Number") },
            placeholder = { Text("+91 1234567890") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            isError = !isPhoneValid,
            supportingText = {
                if (!isPhoneValid) {
                    Text("Please enter a valid phone number")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        val activity = rememberActivity()

        Button(
            onClick = {
                activity?.let {
                    viewModel.sendOtpToPhone(
                        phoneNumber = phoneNumber,
                        activity = it
                    )
                }
            },
            enabled = phoneNumber.isNotBlank() && isPhoneValid && !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send Verification Code")
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