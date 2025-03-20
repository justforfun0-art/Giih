package com.example.gigwork

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import com.example.gigwork.domain.models.UserProfile
import com.example.gigwork.domain.models.UserRole
import com.example.gigwork.presentation.GigWorkApp
import com.example.gigwork.presentation.navigation.NavigationBuilders
import com.example.gigwork.presentation.navigation.Screen
import com.example.gigwork.presentation.theme.GigWorkTheme
import com.example.gigwork.presentation.viewmodels.AuthEvent
import com.example.gigwork.presentation.viewmodels.AuthViewModel
import com.example.gigwork.presentation.viewmodels.UserViewModel
import com.example.gigwork.util.Constants
import com.example.gigwork.util.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.NavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var logger: Logger

    private val viewModel: UserViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    companion object {
        private const val TAG = "MainActivity"
        private const val KEY_PHONE_NUMBER = "phoneNumber"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logger.d(
            tag = TAG,
            message = "Initializing MainActivity",
            additionalData = mapOf(
                "savedInstanceState" to (savedInstanceState != null),
                "timestamp" to System.currentTimeMillis(),
                "deviceId" to getUniqueDeviceId()
            )
        )

        try {
            setContent {
                GigWorkTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    val authUiState by authViewModel.uiState.collectAsState()
                    val navController = rememberNavController()

                    // Get ImageLoader
                    val imageLoader = ImageLoader.Builder(applicationContext)
                        .crossfade(true)
                        .build()

                    // Observe auth events for navigation
                    LaunchedEffect(authViewModel) {
                        authViewModel.events.collect { event ->
                            when (event) {
                                is AuthEvent.NavigateToWelcome -> {
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                                is AuthEvent.NavigateToJobsScreen -> {
                                    navController.navigate(Screen.Jobs.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                                is AuthEvent.NavigateToEmployerDashboard -> {
                                    navController.navigate(Screen.EmployerDashboard.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                                // For NavigateToVerification event
                                is AuthEvent.NavigateToVerification -> {
                                    navController.navigate(
                                        Screen.OtpVerification.createRoute(
                                            phoneNumber = event.phoneNumber,
                                            verificationId = event.verificationId,
                                            userType = authViewModel.getUserType() // Get user type from ViewModel
                                        )
                                    )
                                }
                                // For CodeSent event
                                is AuthEvent.CodeSent -> {
                                    // Since CodeSent doesn't carry parameters, get them from ViewModel state
                                    val phoneNumber = authViewModel.uiState.value.lastPhoneNumber ?: ""
                                    val verificationId = authViewModel.uiState.value.verificationId ?: ""
                                    val userType = authViewModel.getUserType()

                                    navController.navigate(
                                        Screen.OtpVerification.createRoute(
                                            phoneNumber = phoneNumber,
                                            verificationId = verificationId,
                                            userType = userType
                                        )
                                    )
                                }
                                is AuthEvent.NavigateToProfileSetup -> {
                                    // Navigate to profile setup with user type from the auth flow
                                    navController.navigate(
                                        Screen.CreateProfile.createRoute(
                                            userId = event.userId,
                                            userType = event.userType
                                        )
                                    ) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                                is AuthEvent.NavigateToPhoneEntry -> {
                                    // When navigating to phone entry, preserve the user type
                                    val userType = authViewModel.getUserType()
                                    navController.navigate(Screen.PhoneEntry.createRouteWithUserType(userType))
                                }
                                else -> {
                                    logger.d(
                                        tag = TAG,
                                        message = "Unhandled auth event",
                                        additionalData = mapOf("event_type" to event.javaClass.simpleName)
                                    )
                                }
                            }
                        }
                    }

                    // Use AppNavigation for navigation
                    NavHost(
                        navController = navController,
                        startDestination = if (authUiState.isLoggedIn) {
                            when (authUiState.authState?.userType) {
                                Constants.UserType.EMPLOYEE -> Screen.Jobs.route
                                Constants.UserType.EMPLOYER -> Screen.EmployerDashboard.route
                                else -> Screen.Welcome.route
                            }
                        } else {
                            Screen.Welcome.route
                        }
                    ) {
                        with(NavigationBuilders) {
                            addWelcomeScreen(navController)
                            addAuthFlow(navController)
                            addProfileFlow(navController)
                            addMainFlow(navController, imageLoader)
                            addCommonScreens(navController)
                        }
                    }
                    var showErrorDialog by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf("") }

                    if (showErrorDialog) {
                        AlertDialog(
                            onDismissRequest = { showErrorDialog = false },
                            title = { Text("Error") },
                            text = { Text(errorMessage) },
                            confirmButton = {
                                TextButton(onClick = { showErrorDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    // Determine user role based on auth state
                    val userRole = if (authUiState.isLoggedIn) {
                        when (authUiState.authState?.userType) {
                            Constants.UserType.EMPLOYER -> UserRole.EMPLOYER
                            Constants.UserType.EMPLOYEE -> UserRole.EMPLOYEE
                            else -> UserRole.GUEST
                        }
                    } else {
                        UserRole.GUEST
                    }

                    GigWorkApp(
                        userRole = userRole,
                        onUserAction = { action ->
                            handleUserAction(action, navController)
                        },
                        onError = { error ->
                            handleError(error)
                            errorMessage = error.message ?: "Unknown error"
                            showErrorDialog = true
                        },
                        navController = navController
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

    private fun handleUserAction(action: UserAction, navController: androidx.navigation.NavController) {
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
                // Auth-specific actions
                is UserAction.Login -> {
                    // Pass the user type to login screen
                    val userType = authViewModel.getUserType()
                    navController.navigate(Screen.Login.createRoute(userType))
                }
                is UserAction.VerifyOtp -> authViewModel.verifyCode(action.otp)
                is UserAction.NavigateToOtpVerification -> {
                    // Get verification ID and user type from the authViewModel
                    val verificationId = authViewModel.uiState.value.verificationId ?: ""
                    val userType = authViewModel.getUserType()

                    navController.navigate(
                        Screen.OtpVerification.createRoute(
                            phoneNumber = action.phoneNumber,
                            verificationId = verificationId,
                            userType = userType
                        )
                    )
                }
                is UserAction.SendOtp -> {
                    authViewModel.sendOtpToPhone(action.phoneNumber)
                }
                is UserAction.Logout -> authViewModel.logout()

                // User data actions
                is UserAction.Refresh -> viewModel.loadUserData(action.userId)
                is UserAction.UpdateProfile -> viewModel.updateUserProfile(action.profile)

                // Navigation actions with improved flow
                is UserAction.NavigateToEmployeeAuth -> {
                    // Set user type and navigate to appropriate screen
                    authViewModel.setUserType(Constants.UserType.EMPLOYEE)
                    navController.navigate(Screen.Login.createRoute(Constants.UserType.EMPLOYEE))
                }
                is UserAction.NavigateToEmployerAuth -> {
                    // Set user type and navigate to appropriate screen
                    authViewModel.setUserType(Constants.UserType.EMPLOYER)
                    navController.navigate(Screen.Login.createRoute(Constants.UserType.EMPLOYER))
                }

                is UserAction.Navigate -> navController.navigate(action.route)
                is UserAction.NavigateBack -> navController.navigateUp()
                is UserAction.ViewJob -> navController.navigate(Screen.JobDetails.createRoute(action.jobId))
                is UserAction.CreateJob -> navController.navigate(Screen.CreateJob.route)
                is UserAction.ViewProfile -> navController.navigate(Screen.Profile.createRoute(authViewModel.getUserType()))
                is UserAction.EditProfile -> navController.navigate(Screen.Profile.createRoute(authViewModel.getUserType()) + "/edit")
                is UserAction.OpenSettings -> navController.navigate(Screen.Settings.route)

                // Other actions - handle or log as needed
                is UserAction.ApplyForJob,
                is UserAction.UpdateSetting,
                is UserAction.Search,
                is UserAction.ApplyFilter,
                is UserAction.ReportError -> {
                    logger.d(
                        tag = TAG,
                        message = "Unhandled action",
                        additionalData = mapOf("action_type" to action.javaClass.simpleName)
                    )
                }
            }

            // Update last action timestamp
            lastActionTimestamp = System.currentTimeMillis()
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

    private var sessionStartTime: Long = 0L
    private var lastActionTimestamp: Long = 0L

    private fun getDeviceInfo(): Map<String, Any> {
        return mapOf(
            "manufacturer" to android.os.Build.MANUFACTURER,
            "model" to android.os.Build.MODEL,
            "api_level" to android.os.Build.VERSION.SDK_INT,
            "app_version" to BuildConfig.VERSION_NAME
        )
    }

    @SuppressLint("HardwareIds")
    private fun getUniqueDeviceId(): String {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return androidId ?: "unknown_device_id"
    }

    private fun getSessionDuration(): Long {
        return if (sessionStartTime == 0L) {
            0L
        } else {
            System.currentTimeMillis() - sessionStartTime
        }
    }

    private fun getLastActionTimestamp(): Long {
        return lastActionTimestamp
    }

    override fun onResume() {
        super.onResume()
        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lastActionTimestamp = System.currentTimeMillis()
    }

    private fun showErrorDialog(error: Throwable) {
        setContent {
            GigWorkTheme {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Error") },
                    text = { Text(error.message ?: "Unknown error") },
                    confirmButton = {
                        TextButton(onClick = { recreate() }) {
                            Text("Restart App")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { finish() }) {
                            Text("Close App")
                        }
                    }
                )
            }
        }
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