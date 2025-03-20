// presentation/components/jobs/JobsTopAppBar.kt
package com.example.gigwork.presentation.components.jobs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsTopAppBar(
    onFilterClick: () -> Unit,
    onProfileClick: () -> Unit,
    showFilters: Boolean
) {
    TopAppBar(
        title = { Text("Available Jobs") },
        actions = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    imageVector = if (showFilters) Icons.Default.FilterList else Icons.Default.FilterListOff,
                    contentDescription = if (showFilters) "Hide filters" else "Show filters"
                )
            }
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            }
        }
    )
}