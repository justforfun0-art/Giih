// presentation/navigation/components/NavigationItems.kt
package com.example.gigwork.presentation.navigation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.R


data class NavigationItem(
    val route: String,
    val icon: ImageVector,
    val label: Int, // Changed to Int to reference string resource
    val contentDescription: String? = null
)

object NavigationItems {
    fun getItems(userRole: UserRole): List<NavigationItem> {
        return when (userRole) {
            UserRole.EMPLOYER -> listOf(
                NavigationItem(
                    route = Screen.EmployerDashboard.route,
                    icon = Icons.Default.Dashboard,
                    label = R.string.dashboard
                ),
                NavigationItem(
                    route = Screen.EmployerJobs.route,
                    icon = Icons.Default.Work,
                    label = R.string.my_jobs
                ),
                NavigationItem(
                    route = Screen.EmployerJobs.route,
                    icon = Icons.Default.Description,
                    label = R.string.applications
                )
            )
            UserRole.EMPLOYEE -> listOf(
                NavigationItem(
                    route = Screen.Jobs.route,
                    icon = Icons.Default.Search,
                    label = R.string.find_jobs
                ),
                NavigationItem(
                    route = Screen.Jobs.route,
                    icon = Icons.Default.Assignment,
                    label = R.string.my_applications
                )
            )
            UserRole.GUEST -> emptyList()
        }
    }
}