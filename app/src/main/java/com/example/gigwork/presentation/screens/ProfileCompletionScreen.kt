package com.example.gigwork.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.core.error.ui.ErrorDialog
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.domain.models.UserType
import com.example.gigwork.presentation.viewmodels.ProfileViewModel
import androidx.compose.foundation.layout.Row

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionScreen(
    onProfileCompleted: (UserRole) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Handle successful profile creation
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            // Determine user role from the saved profile data
            val userRole = when (state.userType) {
                "EMPLOYER" -> UserRole.EMPLOYER
                else -> UserRole.EMPLOYEE
            }

            onProfileCompleted(userRole)
        }
    }

    // Show error dialog if there's an error
    state.errorMessage?.let { error ->
        ErrorDialog(
            errorMessage = error,
            onDismiss = { viewModel.onErrorDismiss() },
            onAction = viewModel::onErrorAction
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile icon
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Just a few more details to get started",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Full Name Field
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                isError = state.validationErrors.containsKey("name"),
                supportingText = {
                    if (state.validationErrors.containsKey("name")) {
                        Text(
                            text = state.validationErrors["name"] ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field (Optional)
            OutlinedTextField(
                value = state.email ?: "",
                onValueChange = { viewModel.updateEmail(it) },
                label = { Text("Email (Optional)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                isError = state.validationErrors.containsKey("email"),
                supportingText = {
                    if (state.validationErrors.containsKey("email")) {
                        Text(
                            text = state.validationErrors["email"] ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User type selection
            UserTypeSelector(
                selectedUserType = if (state.userType == "EMPLOYER") UserType.EMPLOYER else UserType.EMPLOYEE,
                onUserTypeSelected = { userType ->
                    viewModel.updateUserType(userType.name)
                }
            )

            // For employers, add company name field
            if (state.userType == "EMPLOYER") {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = state.companyName ?: "",
                    onValueChange = { viewModel.updateCompanyName(it) },
                    label = { Text("Company Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    isError = state.validationErrors.containsKey("companyName"),
                    supportingText = {
                        if (state.validationErrors.containsKey("companyName")) {
                            Text(
                                text = state.validationErrors["companyName"] ?: "",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.saveProfile() },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Complete Profile")
                }
            }
        }
    }
}

@Composable
fun UserTypeSelector(
    selectedUserType: UserType,
    onUserTypeSelected: (UserType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "I am a:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.RadioButton(
                selected = selectedUserType == UserType.EMPLOYEE,
                onClick = { onUserTypeSelected(UserType.EMPLOYEE) }
            )
            Text("Employee (Job Seeker)")

            Spacer(modifier = Modifier.width(16.dp))

            androidx.compose.material3.RadioButton(
                selected = selectedUserType == UserType.EMPLOYER,
                onClick = { onUserTypeSelected(UserType.EMPLOYER) }
            )
            Text("Employer (Hiring)")
        }
    }
}
