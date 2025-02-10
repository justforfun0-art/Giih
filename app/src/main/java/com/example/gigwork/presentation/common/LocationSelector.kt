// presentation/common/LocationSelector.kt
package com.example.gigwork.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.presentation.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelector(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = hiltViewModel(),
    onLocationSelected: (state: String, district: String) -> Unit
) {
    val states by viewModel.states.collectAsState()
    val districts by viewModel.districts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedState by remember { mutableStateOf<String?>(null) }
    var selectedDistrict by remember { mutableStateOf<String?>(null) }
    var isStateMenuExpanded by remember { mutableStateOf(false) }
    var isDistrictMenuExpanded by remember { mutableStateOf(false) }

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
                value = selectedState ?: "",
                onValueChange = { },
                readOnly = true,
                label = { Text("State") },
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
                states.forEach { state ->
                    DropdownMenuItem(
                        text = { Text(state) },
                        onClick = {
                            selectedState = state
                            selectedDistrict = null
                            isStateMenuExpanded = false
                            viewModel.loadDistricts(state)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // District Dropdown
        if (selectedState != null) {
            ExposedDropdownMenuBox(
                expanded = isDistrictMenuExpanded,
                onExpandedChange = { isDistrictMenuExpanded = !isDistrictMenuExpanded }
            ) {
                OutlinedTextField(
                    value = selectedDistrict ?: "",
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
                    districts.forEach { district ->
                        DropdownMenuItem(
                            text = { Text(district) },
                            onClick = {
                                selectedDistrict = district
                                isDistrictMenuExpanded = false
                                selectedState?.let { state ->
                                    onLocationSelected(state, district)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Loading State
        if (isLoading) {
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
        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun LocationSelectorPreview() {
    MaterialTheme {
        LocationSelector(
            onLocationSelected = { state, district ->
                // Handle selection in preview
            }
        )
    }
}