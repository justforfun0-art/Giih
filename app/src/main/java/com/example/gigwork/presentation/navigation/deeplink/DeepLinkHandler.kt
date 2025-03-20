package com.example.gigwork.presentation.navigation.deeplink

import android.net.Uri
import com.example.gigwork.di.IoDispatcher
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.navigation.NavigationCommands
import com.example.gigwork.presentation.NavigationScope
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.navigation.NavigationTrigger
import com.example.gigwork.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkHandler @Inject constructor(
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @NavigationScope private val navigationScope: CoroutineScope,
    private val navigationCommands: NavigationCommands
) {
    suspend fun handleDeepLink(uri: Uri, userRole: UserRole) {
        try {
            val route = createRouteFromUri(uri)
            val parameters = extractParameters(uri)

            logger.d(
                tag = TAG,
                message = "Processing deep link",
                additionalData = mapOf(
                    "uri" to uri.toString(),
                    "route" to route,
                    "parameters" to parameters,
                    "userRole" to userRole.name
                )
            )

            // Use navigationCommands instead of navigationManager
            navigationCommands.navigateTo(route)

        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Deep link processing failed",
                throwable = e,
                additionalData = mapOf(
                    "uri" to uri.toString(),
                    "userRole" to userRole.name
                )
            )
            throw e
        }
    }

    // Keep the rest of your methods the same
    private fun createRouteFromUri(uri: Uri): String {
        // Your existing implementation
        return when (uri.path) {
            "/jobs" -> Screen.Jobs.route
            "/job-details" -> {
                val jobId = requireNotNull(uri.getQueryParameter("jobId")) {
                    "Job ID is required for job details"
                }
                Screen.JobDetails.createRoute(jobId)
            }
            "/employer/profile" -> {
                val employerId = requireNotNull(uri.getQueryParameter("employerId")) {
                    "Employer ID is required for employer profile"
                }
                Screen.EmployerProfile.createRoute(employerId)
            }
            "/employee/profile" -> {
                val employeeId = requireNotNull(uri.getQueryParameter("employeeId")) {
                    "Employee ID is required for employee profile"
                }
                Screen.EmployeeProfile.createRoute(employeeId)
            }
            else -> Screen.Welcome.route
        }
    }

    private fun extractParameters(uri: Uri): Map<String, String> {
        return uri.queryParameterNames.associateWith { paramName ->
            uri.getQueryParameter(paramName) ?: ""
        }
    }

    companion object {
        private const val TAG = "DeepLinkHandler"
    }
}