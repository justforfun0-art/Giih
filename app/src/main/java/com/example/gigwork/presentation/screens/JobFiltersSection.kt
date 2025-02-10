// presentation/screens/JobFiltersSection.kt
package com.example.gigwork.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gigwork.presentation.common.LocationSelector
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gigwork.presentation.viewmodels.LocationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFiltersSection(
    selectedState: String?,
    selectedDistrict: String?,
    minSalary: Double?,
    maxSalary: Double?,
    onApplyFilters: (String?, String?, Double?, Double?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempMinSalary by remember(minSalary) {
        mutableStateOf(minSalary?.toString() ?: "")
    }
    var tempMaxSalary by remember(maxSalary) {
        mutableStateOf(maxSalary?.toString() ?: "")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = onClearFilters) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Filters"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Selection
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LocationSelector(
                onLocationSelected = { state, district ->
                    onApplyFilters(
                        state,
                        district,
                        tempMinSalary.toDoubleOrNull(),
                        tempMaxSalary.toDoubleOrNull()
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Salary Range
            Text(
                text = "Salary Range",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tempMinSalary,
                    onValueChange = { tempMinSalary = it },
                    label = { Text("Min Salary") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    prefix = { Text("₹") }
                )

                OutlinedTextField(
                    value = tempMaxSalary,
                    onValueChange = { tempMaxSalary = it },
                    label = { Text("Max Salary") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    prefix = { Text("₹") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var selectedDuration by remember { mutableStateOf<String?>(null) }

                FilterChip(
                    selected = selectedDuration == "hourly",
                    onClick = { selectedDuration = "hourly" },
                    label = { Text("Hourly") }
                )

                FilterChip(
                    selected = selectedDuration == "daily",
                    onClick = { selectedDuration = "daily" },
                    label = { Text("Daily") }
                )

                FilterChip(
                    selected = selectedDuration == "weekly",
                    onClick = { selectedDuration = "weekly" },
                    label = { Text("Weekly") }
                )

                FilterChip(
                    selected = selectedDuration == "monthly",
                    onClick = { selectedDuration = "monthly" },
                    label = { Text("Monthly") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    onApplyFilters(
                        selectedState,
                        selectedDistrict,
                        tempMinSalary.toDoubleOrNull(),
                        tempMaxSalary.toDoubleOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<MutableList<Int>>()
        val itemConstraints = constraints.copy(minWidth = 0)

        val placeables = measurables.map { measurable ->
            measurable.measure(itemConstraints)
        }

        var currentRow = mutableListOf<Int>()
        var currentRowWidth = 0

        placeables.forEachIndexed { index, placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(index)
                currentRowWidth = placeable.width
            } else {
                currentRow.add(index)
                currentRowWidth += placeable.width
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        val height = rows.sumOf { row ->
            row.maxOf { placeables[it].height }
        }

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val maxHeight = row.maxOf { placeables[it].height }

                row.forEach { index ->
                    val placeable = placeables[index]
                    placeable.place(
                        x = x,
                        y = y + (maxHeight - placeable.height) / 2
                    )
                    x += placeable.width
                }
                y += maxHeight
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun JobFiltersSectionPreview() {
    MaterialTheme {
        JobFiltersSection(
            selectedState = null,
            selectedDistrict = null,
            minSalary = null,
            maxSalary = null,
            onApplyFilters = { _, _, _, _ -> },
            onClearFilters = { }
        )
    }
}