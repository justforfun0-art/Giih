package com.example.gigwork.presentation.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gigwork.presentation.common.LocationSelector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobFiltersSection(
    selectedState: String?,
    selectedDistrict: String?,
    minSalary: Double?,
    maxSalary: Double?,
    onApplyFilters: (String?, String?, Double?, Double?, String?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tempMinSalary by remember(minSalary) {
        mutableStateOf(minSalary?.toString() ?: "")
    }
    var tempMaxSalary by remember(maxSalary) {
        mutableStateOf(maxSalary?.toString() ?: "")
    }
    var selectedDuration by remember { mutableStateOf<String?>(null) }

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
            FilterHeader(onClearFilters)

            Spacer(modifier = Modifier.height(16.dp))

            LocationSection(
                selectedState = selectedState,
                selectedDistrict = selectedDistrict,
                onLocationSelected = { state, district ->
                    onApplyFilters(
                        state,
                        district,
                        tempMinSalary.toDoubleOrNull(),
                        tempMaxSalary.toDoubleOrNull(),
                        selectedDuration
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SalaryRangeSection(
                tempMinSalary = tempMinSalary,
                tempMaxSalary = tempMaxSalary,
                onMinSalaryChange = { tempMinSalary = it },
                onMaxSalaryChange = { tempMaxSalary = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DurationFilterSection(
                selectedDuration = selectedDuration,
                onDurationSelected = { selectedDuration = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onApplyFilters(
                        selectedState,
                        selectedDistrict,
                        tempMinSalary.toDoubleOrNull(),
                        tempMaxSalary.toDoubleOrNull(),
                        selectedDuration
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
private fun FilterHeader(onClearFilters: () -> Unit) {
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
}

@Composable
private fun LocationSection(
    selectedState: String?,
    selectedDistrict: String?,
    onLocationSelected: (String?, String?) -> Unit
) {
    Text(
        text = "Location",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    LocationSelector(
        selectedState = selectedState ?: "",
        selectedDistrict = selectedDistrict ?: "",
        onLocationSelected = { state, district ->
            onLocationSelected(
                state.takeIf { it.isNotEmpty() },
                district.takeIf { it.isNotEmpty() }
            )
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalaryRangeSection(
    tempMinSalary: String,
    tempMaxSalary: String,
    onMinSalaryChange: (String) -> Unit,
    onMaxSalaryChange: (String) -> Unit
) {
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
            onValueChange = onMinSalaryChange,
            label = { Text("Min Salary") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            prefix = { Text("₹") }
        )
        OutlinedTextField(
            value = tempMaxSalary,
            onValueChange = onMaxSalaryChange,
            label = { Text("Max Salary") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            prefix = { Text("₹") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationFilterSection(
    selectedDuration: String?,
    onDurationSelected: (String) -> Unit
) {
    Text(
        text = "Duration",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("hourly", "daily", "weekly", "monthly").forEach { duration ->
            FilterChip(
                selected = selectedDuration == duration,
                onClick = { onDurationSelected(duration) },
                label = { Text(duration.capitalize()) },
                modifier = Modifier.padding(end = 8.dp)
            )
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
    ) { measurables: List<Measurable>, constraints: Constraints ->
        val spacingPx = 8.dp.roundToPx()
        val rowPlacements = mutableListOf<RowItemPlacement>()
        var currentX = 0
        var currentY = 0
        var rowMaxHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(
                Constraints(
                    maxWidth = constraints.maxWidth,
                    maxHeight = constraints.maxHeight
                )
            )

            if (currentX + placeable.width > constraints.maxWidth && currentX > 0) {
                currentY += rowMaxHeight + spacingPx
                currentX = 0
                rowMaxHeight = 0
            }

            rowPlacements.add(
                RowItemPlacement(
                    x = currentX,
                    y = currentY,
                    placeable = placeable
                )
            )

            currentX += placeable.width + spacingPx
            rowMaxHeight = max(rowMaxHeight, placeable.height)
        }

        val totalHeight = if (rowPlacements.isEmpty()) {
            0
        } else {
            rowPlacements.maxOf { it.y + it.placeable.height }
        }

        layout(constraints.maxWidth, totalHeight) {
            rowPlacements.forEach { placement ->
                placement.placeable.placeRelative(
                    x = placement.x,
                    y = placement.y
                )
            }
        }
    }
}

private data class RowItemPlacement(
    val x: Int,
    val y: Int,
    val placeable: Placeable
)

private data class RowPlacement(
    val x: Int,
    val y: Int,
    val placeable: Placeable
)
@Preview(showBackground = true)
@Composable
fun JobFiltersSectionPreview() {
    MaterialTheme {
        JobFiltersSection(
            selectedState = null,
            selectedDistrict = null,
            minSalary = null,
            maxSalary = null,
            onApplyFilters = { _, _, _, _, _ -> },
            onClearFilters = { }
        )
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { it.uppercase() }
}