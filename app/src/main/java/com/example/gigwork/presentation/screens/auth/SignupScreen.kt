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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gigwork.core.error.model.ErrorLevel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.presentation.common.LoadingButton
import com.example.gigwork.presentation.screens.common.ScreenScaffold
import com.example.gigwork.presentation.viewmodels.SignupViewModel
import com.example.gigwork.util.Constants

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SignupScreen(
    userType: String,
    onNavigateToOtpVerification: (String, String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SignupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current as? ComponentActivity

    // Set user type in the view model
    LaunchedEffect(userType) {
        viewModel.setUserType(userType)
    }

    // Effect to handle OTP events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignupViewModel.SignupEvent.OtpSent -> {
                    onNavigateToOtpVerification(phone, event.verificationId)
                }
                else -> {}
            }
        }
    }

    ScreenScaffold(
        errorMessage = uiState.errorMessage,
        onErrorDismiss = { viewModel.clearError() },
        onErrorAction = { viewModel.handleErrorAction(it) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (userType == Constants.UserType.EMPLOYER)
                            "Employer Signup"
                        else
                            "Job Seeker Signup"
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
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = if (userType == Constants.UserType.EMPLOYER)
                        "Create Employer Account"
                    else
                        "Create Job Seeker Account",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // User Type Selection
                UserTypeSelection(
                    isEmployer = userType == Constants.UserType.EMPLOYER,
                    onUserTypeChanged = { isEmployer ->
                        viewModel.setUserType(if (isEmployer) Constants.UserType.EMPLOYER else Constants.UserType.EMPLOYEE)
                    }
                )

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text(
                            if (userType == Constants.UserType.EMPLOYER)
                                "Company Name"
                            else
                                "Full Name"
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (userType == Constants.UserType.EMPLOYER)
                                Icons.Default.Business
                            else
                                Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    isError = "name" in uiState.validationErrors,
                    supportingText = {
                        uiState.validationErrors["name"]?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Field
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        if (it.length <= 15 && (it.isEmpty() || it.all { c -> c.isDigit() || c == '+' })) {
                            phone = it
                        }
                    },
                    label = { Text("Phone Number") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (name.isNotBlank() && phone.isNotBlank() && phone.length >= 10 && acceptedTerms) {
                                viewModel.signupWithPhone(name, phone, acceptedTerms)
                            }
                        }
                    ),
                    singleLine = true,
                    isError = "phone" in uiState.validationErrors,
                    supportingText = {
                        if ("phone" in uiState.validationErrors) {
                            Text(uiState.validationErrors["phone"] ?: "", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Enter your phone number with country code")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Terms and Conditions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = acceptedTerms,
                        onCheckedChange = { acceptedTerms = it }
                    )
                    Column {
                        Text(
                            text = "I accept the Terms and Conditions",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if ("terms" in uiState.validationErrors) {
                            Text(
                                text = uiState.validationErrors["terms"] ?: "You must accept the terms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Validation errors summary (for errors not tied to specific fields)
                if (uiState.validationErrors.isNotEmpty()) {
                    val generalErrors = uiState.validationErrors.filter { (key, _) ->
                        key !in listOf("name", "phone", "terms")
                    }

                    if (generalErrors.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Please fix the following errors:",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                generalErrors.forEach { (_, error) ->
                                    Row {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            error,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Signup Button
                LoadingButton(
                    onClick = {
                        keyboardController?.hide()
                        viewModel.signupWithPhone(name, phone, acceptedTerms)
                    },
                    text = "Continue to Verification",
                    isLoading = uiState.isLoading,
                    enabled = name.isNotBlank() && phone.isNotBlank() && phone.length >= 10 && acceptedTerms && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Login Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account?")
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Log In")
                    }
                }
            }

            // Critical Error Dialog
            uiState.errorMessage?.let { error ->
                if (error.level == ErrorLevel.CRITICAL) {
                    ErrorDialog(
                        errorMessage = error,
                        onDismiss = { viewModel.clearError() },
                        onAction = { viewModel.handleErrorAction(it) },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTypeSelection(
    isEmployer: Boolean,
    onUserTypeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign up as",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !isEmployer,
                onClick = { onUserTypeChanged(false) },
                label = { Text("Job Seeker") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                modifier = Modifier.weight(1f)
            )

            FilterChip(
                selected = isEmployer,
                onClick = { onUserTypeChanged(true) },
                label = { Text("Employer") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}