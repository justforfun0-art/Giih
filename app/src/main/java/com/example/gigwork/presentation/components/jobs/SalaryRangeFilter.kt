// presentation/components/jobs/SalaryRangeFilter.kt
package com.example.gigwork.presentation.components.jobs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalaryRangeFilter(
    minSalary: Int?,
    maxSalary: Int?,
    onMinSalaryChange: (Int?) -> Unit,
    onMaxSalaryChange: (Int?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Salary Range",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minSalary?.toString() ?: "",
                onValueChange = { value ->
                    onMinSalaryChange(value.toIntOrNull())
                },
                modifier = Modifier.weight(1f),
                label = { Text("Min Salary") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = maxSalary?.toString() ?: "",
                onValueChange = { value ->
                    onMaxSalaryChange(value.toIntOrNull())
                },
                modifier = Modifier.weight(1f),
                label = { Text("Max Salary") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}