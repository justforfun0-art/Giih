package com.example.gigwork.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.presentation.viewmodels.LocationViewModel
import com.example.gigwork.presentation.states.ValidationError as UiValidationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelector(
    selectedState: String = "",
    selectedDistrict: String = "",
    onLocationSelected: (state: String, district: String) -> Unit,
    error: UiValidationError? = null,  // Add this parameter, which represents the error state
    viewModel: LocationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var isStateMenuExpanded by remember { mutableStateOf(false) }
    var isDistrictMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStates()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // State Dropdown
        ExposedDropdownMenuBox(
            expanded = isStateMenuExpanded,
            onExpandedChange = { isStateMenuExpanded = !isStateMenuExpanded }
        ) {
            OutlinedTextField(
                value = selectedState,
                onValueChange = { },
                readOnly = true,
                label = { Text("State") },
                isError = error != null,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStateMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = isStateMenuExpanded,
                onDismissRequest = { isStateMenuExpanded = false }
            ) {
                state.states.forEach { stateOption ->
                    DropdownMenuItem(
                        text = { Text(stateOption) },
                        onClick = {
                            viewModel.updateSelectedState(stateOption)
                            isStateMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // District Dropdown
        if (selectedState.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = isDistrictMenuExpanded,
                onExpandedChange = { isDistrictMenuExpanded = !isDistrictMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDistrict,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("District") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDistrictMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = isDistrictMenuExpanded,
                    onDismissRequest = { isDistrictMenuExpanded = false }
                ) {
                    state.districts.forEach { districtOption ->
                        DropdownMenuItem(
                            text = { Text(districtOption) },
                            onClick = {
                                viewModel.updateSelectedDistrict(districtOption)
                                isDistrictMenuExpanded = false
                                onLocationSelected(selectedState, districtOption)
                            }
                        )
                    }
                }
            }
        }

        // Display validation error if present
        error?.let {
            Text(
                text = it.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }


        // Loading State
        if (state.isLoadingStates || state.isLoadingDistricts) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Error State
        state.errorMessage?.let { error ->
            Text(
                text = error.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }


    }
}