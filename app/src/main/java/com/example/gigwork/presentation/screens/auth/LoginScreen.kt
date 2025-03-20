package com.example.gigwork.presentation.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.presentation.common.LoadingButton
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.MobileVerificationViewModel
import com.example.gigwork.presentation.viewmodels.VerificationEvent
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun LoginScreen(
    userType: String,
    onNavigateBack: () -> Unit,
    onNavigateToOtpVerification: (String, String) -> Unit,
    viewModel: MobileVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var phoneNumber by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    var rememberMe by remember { mutableStateOf(false) }
    val context = LocalContext.current as? ComponentActivity

    // Handle verification events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is VerificationEvent.NavigateToProfileSetup,
                is VerificationEvent.NavigateToJobsScreen,
                is VerificationEvent.NavigateToEmployerDashboard -> {
                    // These will be handled after OTP verification
                }
            }
        }
    }

    // Monitor for verification ID
    LaunchedEffect(uiState.verificationId) {
        uiState.verificationId?.let { verificationId ->
            if (verificationId.isNotEmpty() && uiState.isCodeSent) {
                onNavigateToOtpVerification(phoneNumber, verificationId)
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = { viewModel._uiState.update { it.copy(errorMessage = null) } },
        onErrorAction = { /* Handle error action if needed */ },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (userType == UserType.EMPLOYER.name)
                            "Employer Login"
                        else
                            "Job Seeker Login"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // User Type Display
                DisplayUserType(userType = userType)

                Spacer(modifier = Modifier.height(32.dp))

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        if (it.length <= 15 && (it.isEmpty() || it.all { c -> c.isDigit() || c == '+' })) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+910000000000") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (phoneNumber.isNotBlank() && phoneNumber.length >= 10 && !uiState.isLoading) {
                                viewModel.sendVerificationCode(phoneNumber)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Enter your phone number with country code") },
                    isError = uiState.errorMessage?.message?.contains("phone", ignoreCase = true) == true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Remember Me Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                    Text("Remember my phone number")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Send OTP Button
                LoadingButton(
                    onClick = {
                        // Pass the activity if available
                        context?.let {
                            viewModel.sendVerificationCode(
                                phoneNumber = phoneNumber,
                                activity = it
                            )
                        } ?: run {
                            // Handle case where no activity is available
                            viewModel.sendVerificationCode(phoneNumber)
                        }
                    },
                    text = "Send Verification Code",
                    isLoading = uiState.isLoading,
                    enabled = phoneNumber.isNotBlank() && phoneNumber.length >= 10 && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Sign Up Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account?")
                    TextButton(onClick = onNavigateBack) {
                        Text("Sign Up")
                    }
                }
            }

            // Critical Error Dialog
            uiState.errorMessage?.let { error ->
                if (error.level == ErrorLevel.CRITICAL) {
                    ErrorDialog(
                        errorMessage = error,
                        onDismiss = { viewModel._uiState.update { it.copy(errorMessage = null) } },
                        onAction = { /* Handle error action */ },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayUserType(userType: String, modifier: Modifier = Modifier) {
    val isEmployer = userType == UserType.EMPLOYER.name

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login as",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val icon = if (isEmployer)
                Icons.Default.Business else Icons.Default.Person
            val text = if (isEmployer) "Employer" else "Job Seeker"

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}