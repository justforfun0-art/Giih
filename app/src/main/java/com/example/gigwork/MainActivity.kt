// MainActivity.kt
package com.example.gigwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.gigwork.presentation.theme.GigWorkTheme
import com.example.gigwork.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var logger: Logger

    private val viewModel: UserViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.d(
            tag = TAG,
            message = "Initializing MainActivity",
            additionalData = mapOf(
                "savedInstanceState" to (savedInstanceState != null),
                "timestamp" to System.currentTimeMillis(),
                "deviceId" to getDeviceId()
            )
        )

        try {
            setContent {
                GigWorkTheme {
                    val uiState by viewModel.uiState.collectAsState()

                    MainScreen(
                        uiState = uiState,
                        onUserAction = ::handleUserAction,
                        onError = ::handleError
                    )
                }
            }

            logger.i(
                tag = TAG,
                message = "MainActivity UI setup complete",
                additionalData = mapOf(
                    "screenWidth" to resources.displayMetrics.widthPixels,
                    "screenHeight" to resources.displayMetrics.heightPixels
                )
            )
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Failed to initialize MainActivity",
                throwable = e,
                additionalData = mapOf(
                    "error_type" to e.javaClass.simpleName,
                    "device_info" to getDeviceInfo()
                )
            )
            showErrorDialog(e)
        }
    }

    private fun handleUserAction(action: UserAction) {
        logger.d(
            tag = TAG,
            message = "User action received",
            additionalData = mapOf(
                "action_type" to action.javaClass.simpleName,
                "timestamp" to System.currentTimeMillis()
            )
        )

        try {
            when (action) {
                is UserAction.Refresh -> viewModel.loadUserData(action.userId)
                is UserAction.UpdateProfile -> viewModel.updateUserProfile(action.profile)
                is UserAction.Logout -> handleLogout()
            }
        } catch (e: Exception) {
            logger.e(
                tag = TAG,
                message = "Failed to handle user action",
                throwable = e,
                additionalData = mapOf(
                    "action_type" to action.javaClass.simpleName,
                    "error_type" to e.javaClass.simpleName
                )
            )
            showErrorDialog(e)
        }
    }

    private fun handleError(error: Throwable) {
        logger.e(
            tag = TAG,
            message = "Error occurred in MainActivity",
            throwable = error,
            additionalData = mapOf(
                "error_type" to error.javaClass.simpleName,
                "screen_state" to viewModel.uiState.value.javaClass.simpleName
            )
        )
        showErrorDialog(error)
    }

    private fun handleLogout() {
        logger.i(
            tag = TAG,
            message = "User logging out",
            additionalData = mapOf(
                "session_duration" to getSessionDuration(),
                "last_action_timestamp" to getLastActionTimestamp()
            )
        )
        // Logout logic
    }

    private fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "api_level" to android.os.Build.VERSION.SDK_INT,
            "app_version" to BuildConfig.VERSION_NAME
        )
    }

    override fun onDestroy() {
        logger.d(
            tag = TAG,
            message = "MainActivity being destroyed",
            additionalData = mapOf(
                "isFinishing" to isFinishing,
                "session_duration" to getSessionDuration()
            )
        )
        super.onDestroy()
    }
}

sealed class UserAction {
    data class Refresh(val userId: String) : UserAction()
    data class UpdateProfile(val profile: UserProfile) : UserAction()
    object Logout : UserAction()
}