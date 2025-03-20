package com.example.gigwork.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.example.gigwork.R
import com.example.gigwork.presentation.navigation.Screen

@Composable
fun GigWorkBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.home)) },
            selected = currentRoute == Screen.Jobs.route,
            onClick = { onNavigate(Screen.Jobs.route) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Work, contentDescription = null) },
            label = { Text(stringResource(R.string.my_jobs)) },
            selected = currentRoute == Screen.EmployerJobs.route,
            onClick = { onNavigate(Screen.EmployerJobs.route) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text(stringResource(R.string.profile)) },
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) }
        )
    }
}