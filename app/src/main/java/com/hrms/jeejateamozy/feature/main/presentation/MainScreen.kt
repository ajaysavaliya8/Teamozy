package com.hrms.jeejateamozy.feature.main.presentation

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.hrms.jeejateamozy.core.fcm.NotificationEventBus
import com.hrms.jeejateamozy.core.network.PostShiftAction
import com.hrms.jeejateamozy.core.network.PostShiftCheckPayload
import com.hrms.jeejateamozy.core.network.PostShiftRequestBody
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.profile.presentation.*
import com.hrms.jeejateamozy.feature.home.presentation.HomePage
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportScreen
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportViewModel
import com.hrms.jeejateamozy.feature.circular.presentation.CircularViewModel
import com.hrms.jeejateamozy.feature.circular.presentation.CircularListScreen
import com.hrms.jeejateamozy.feature.circular.presentation.CircularDetailScreen
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveViewModel
import com.hrms.jeejateamozy.feature.leave.presentation.ApplyLeaveScreen
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveDetailScreen
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveHistoryScreen
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceHistoryViewModel
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceHistoryScreen
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceDayDetailScreen
import com.hrms.jeejateamozy.feature.attendance.presentation.dialogs.PostShiftCheckDialog
import com.hrms.jeejateamozy.feature.notification.presentation.NotificationListScreen
import com.hrms.jeejateamozy.feature.notification.presentation.NotificationViewModel
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import com.hrms.jeejateamozy.navigation.DeepLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

private const val TAG = "MainScreen"

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    initialDeepLink: DeepLink? = null  // Supports all screen types from notifications
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager.getInstance(context) }

    // ============================================
    // DOUBLE-BACK TO EXIT STATE
    // ============================================
    var backPressedOnce by remember { mutableStateOf(false) }

    // Reset backPressedOnce after 2 seconds
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    // State to track current selected bottom navigation screen
    var currentNavigationScreen by remember { mutableStateOf(NavigationScreen.HOME) }

    // State to manage child screens
    var showEditSocialMedia by remember { mutableStateOf(false) }
    var showEditContactDetail by remember { mutableStateOf(false) }
    var showEditPersonalInfo by remember { mutableStateOf(false) }
    var showViewEmploymentDetail by remember { mutableStateOf(false) }
    var showEditBankingInfo by remember { mutableStateOf(false) }
    var showViewEmploymentIdentity by remember { mutableStateOf(false) }
    var showViewShiftDetails by remember { mutableStateOf(false) }
    var showWorkReport by remember { mutableStateOf(false) }

    var showCircularList by remember { mutableStateOf(false) }
    var showCircularDetail by remember { mutableStateOf<Int?>(null) }

    var showApplyLeave by remember { mutableStateOf(false) }
    var showLeaveHistory by remember { mutableStateOf(false) }
    var showLeaveDetail by remember { mutableStateOf<Int?>(null) }
    var leaveSubmitSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Attendance History States
    var showAttendanceHistory by remember { mutableStateOf(false) }
    var selectedAttendanceDate by remember { mutableStateOf<String?>(null) }

    // ⭐ NEW: Notification List State
    var showNotificationList by remember { mutableStateOf(false) }

    // Post-shift check dialog state
    var postShiftPayload by remember { mutableStateOf<PostShiftCheckPayload?>(null) }
    var postShiftLoading by remember { mutableStateOf(false) }
    var postShiftResult by remember { mutableStateOf<String?>(null) }
    var postShiftIsError by remember { mutableStateOf(false) }

    // Collect post-shift check events from FCM (foreground)
    DisposableEffect(Unit) {
        NotificationEventBus.isPostShiftCollectorActive = true
        onDispose { NotificationEventBus.isPostShiftCollectorActive = false }
    }
    LaunchedEffect(Unit) {
        NotificationEventBus.postShiftCheckEvent.collect { payload ->
            Log.d(TAG, "Post-shift check event received - recordId: ${payload.attendanceRecordId}")
            postShiftPayload = payload
            postShiftLoading = false
            postShiftResult = null
            postShiftIsError = false
        }
    }

    // Check for pending post-shift on resume
    // 1) In-memory (foreground FCM)  2) API call (single source of truth)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && postShiftPayload == null) {
                // In-memory first (instant, no network delay for foreground FCM)
                val pending = NotificationEventBus.pendingPostShiftPayload
                if (pending != null) {
                    Log.d(TAG, "Showing post-shift dialog from in-memory (recordId: ${pending.attendanceRecordId})")
                    postShiftPayload = pending
                    postShiftLoading = false
                    postShiftResult = null
                    postShiftIsError = false
                    NotificationEventBus.clearPendingPostShiftPayload()
                } else {
                    // Ask server if there's a pending post-shift check
                    scope.launch {
                        fetchPostShiftFromServer(
                            token = preferencesManager.authToken.orEmpty(),
                            onResult = { payload ->
                                postShiftPayload = payload
                                postShiftLoading = false
                                postShiftResult = null
                                postShiftIsError = false
                            },
                            onNone = {
                                // Server says no pending check — clear stale local data
                                preferencesManager.clearPostShiftData()
                            }
                        )
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle deep link navigation from notifications
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null) {
            Log.d(TAG, "📱 Processing deep link: $initialDeepLink")

            when (initialDeepLink) {
                // Main tabs
                is DeepLink.Home -> {
                    currentNavigationScreen = NavigationScreen.HOME
                }
                is DeepLink.Attendance -> {
                    currentNavigationScreen = NavigationScreen.ATTENDANCE
                }
                is DeepLink.Profile -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                }

                // Circular screens
                is DeepLink.CircularList -> {
                    showCircularList = true
                }
                is DeepLink.CircularDetail -> {
                    showCircularDetail = initialDeepLink.circularId
                }

                // Leave screens
                is DeepLink.LeaveHistory -> {
                    showLeaveHistory = true
                }
                is DeepLink.ApplyLeave -> {
                    showApplyLeave = true
                }
                is DeepLink.LeaveDetail -> {
                    showLeaveDetail = initialDeepLink.leaveId
                }

                // Attendance detail
                is DeepLink.AttendanceDetail -> {
                    selectedAttendanceDate = initialDeepLink.date
                    showAttendanceHistory = true
                }

                // Other screens
                is DeepLink.WorkReport -> {
                    showWorkReport = true
                }
                is DeepLink.NotificationList -> {
                    showNotificationList = true
                }

                // Profile child screens
                is DeepLink.EditSocialMedia -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showEditSocialMedia = true
                }
                is DeepLink.EditContact -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showEditContactDetail = true
                }
                is DeepLink.EditPersonalInfo -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showEditPersonalInfo = true
                }
                is DeepLink.ViewEmploymentDetail -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showViewEmploymentDetail = true
                }
                is DeepLink.EditBankingInfo -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showEditBankingInfo = true
                }
                is DeepLink.ViewEmploymentIdentity -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showViewEmploymentIdentity = true
                }
                is DeepLink.ViewShiftDetails -> {
                    currentNavigationScreen = NavigationScreen.PROFILE
                    showViewShiftDetails = true
                }

                is DeepLink.PostShiftCheck -> {
                    val parsed = parsePostShiftDataJson(initialDeepLink.postShiftData)
                        ?: NotificationEventBus.pendingPostShiftPayload
                        ?: parsePostShiftDataJson(preferencesManager.postShiftDataJson)
                    if (parsed != null) {
                        postShiftPayload = parsed
                        postShiftLoading = false
                        postShiftResult = null
                        postShiftIsError = false
                    } else {
                        // No local data — fetch from server as fallback
                        scope.launch {
                            fetchPostShiftFromServer(
                                token = preferencesManager.authToken.orEmpty(),
                                onResult = { payload ->
                                    postShiftPayload = payload
                                    postShiftLoading = false
                                    postShiftResult = null
                                    postShiftIsError = false
                                },
                                onNone = {
                                    Log.d(TAG, "Server has no pending post-shift check for deep link")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================================
    // Helper function to check if any child screen is open
    // ============================================
    val isAnyChildScreenOpen = remember(
        selectedAttendanceDate, showCircularDetail, showCircularList,
        showLeaveHistory, showApplyLeave, showWorkReport,
        showEditSocialMedia, showEditContactDetail, showEditPersonalInfo,
        showViewEmploymentDetail, showEditBankingInfo, showViewEmploymentIdentity,
        showViewShiftDetails, showNotificationList  // ⭐ Added showNotificationList
    ) {
        selectedAttendanceDate != null ||
                showCircularDetail != null ||
                showCircularList ||
                showLeaveHistory ||
                showApplyLeave ||
                showWorkReport ||
                showEditSocialMedia ||
                showEditContactDetail ||
                showEditPersonalInfo ||
                showViewEmploymentDetail ||
                showEditBankingInfo ||
                showViewEmploymentIdentity ||
                showViewShiftDetails ||
                showNotificationList  // ⭐ Added
    }

    // ============================================
    // BACK HANDLER - ALWAYS ENABLED
    // ============================================
    BackHandler(enabled = true) {
        when {
            // ⭐ NEW: Close notification list
            showNotificationList -> {
                showNotificationList = false
            }

            // Priority 1: Close any open child screen (step-by-step back)
            selectedAttendanceDate != null -> {
                selectedAttendanceDate = null
                showAttendanceHistory = false
            }
            showCircularDetail != null -> {
                showCircularDetail = null
                showCircularList = true  // Go back to list, not HOME
            }
            showCircularList -> {
                showCircularList = false
            }
            showLeaveHistory -> {
                showLeaveHistory = false
            }
            showApplyLeave -> {
                showApplyLeave = false
                showLeaveHistory = true  // Go back to Leave History instead of Home
            }
            showWorkReport -> {
                showWorkReport = false
            }
            showEditSocialMedia -> {
                showEditSocialMedia = false
            }
            showEditContactDetail -> {
                showEditContactDetail = false
            }
            showEditPersonalInfo -> {
                showEditPersonalInfo = false
            }
            showViewEmploymentDetail -> {
                showViewEmploymentDetail = false
            }
            showEditBankingInfo -> {
                showEditBankingInfo = false
            }
            showViewEmploymentIdentity -> {
                showViewEmploymentIdentity = false
            }
            showViewShiftDetails -> {
                showViewShiftDetails = false
            }

            // Priority 2: Switch to HOME tab if on other tabs
            currentNavigationScreen != NavigationScreen.HOME -> {
                currentNavigationScreen = NavigationScreen.HOME
            }

            // Priority 3: Double-back to exit (only when on HOME with no child screens)
            else -> {
                if (backPressedOnce) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentNavigationScreen,
                onScreenSelected = { screen ->
                    currentNavigationScreen = screen

                    // Reset child navigation when switching tabs
                    showEditSocialMedia = false
                    showEditContactDetail = false
                    showEditPersonalInfo = false
                    showViewEmploymentDetail = false
                    showEditBankingInfo = false
                    showViewEmploymentIdentity = false
                    showViewShiftDetails = false
                    showWorkReport = false
                    showCircularList = false
                    showCircularDetail = null
                    showApplyLeave = false
                    showLeaveHistory = false
                    showAttendanceHistory = false
                    selectedAttendanceDate = null
                    showNotificationList = false  // ⭐ Reset notification list
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        // Handle child screens (full screen overlays)
        when {
            // ⭐ NEW: Notification List Screen
            showNotificationList -> {
                val notificationViewModel: NotificationViewModel = koinViewModel()

                NotificationListScreen(
                    viewModel = notificationViewModel,
                    onNavigateBack = {
                        showNotificationList = false
                    },
                    onNavigateToScreen = { deepLink ->
                        showNotificationList = false
                        // Handle navigation based on deep link type
                        when (deepLink) {
                            is DeepLink.Home -> currentNavigationScreen = NavigationScreen.HOME
                            is DeepLink.Attendance -> currentNavigationScreen = NavigationScreen.ATTENDANCE
                            is DeepLink.Profile -> currentNavigationScreen = NavigationScreen.PROFILE
                            is DeepLink.CircularList -> showCircularList = true
                            is DeepLink.CircularDetail -> showCircularDetail = deepLink.circularId
                            is DeepLink.LeaveHistory -> showLeaveHistory = true
                            is DeepLink.ApplyLeave -> showApplyLeave = true
                            is DeepLink.LeaveDetail -> showLeaveDetail = deepLink.leaveId
                            is DeepLink.AttendanceDetail -> {
                                selectedAttendanceDate = deepLink.date
                                showAttendanceHistory = true
                            }
                            is DeepLink.WorkReport -> showWorkReport = true
                            is DeepLink.NotificationList -> showNotificationList = true
                            is DeepLink.EditSocialMedia -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showEditSocialMedia = true
                            }
                            is DeepLink.EditContact -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showEditContactDetail = true
                            }
                            is DeepLink.EditPersonalInfo -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showEditPersonalInfo = true
                            }
                            is DeepLink.ViewEmploymentDetail -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showViewEmploymentDetail = true
                            }
                            is DeepLink.EditBankingInfo -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showEditBankingInfo = true
                            }
                            is DeepLink.ViewEmploymentIdentity -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showViewEmploymentIdentity = true
                            }
                            is DeepLink.ViewShiftDetails -> {
                                currentNavigationScreen = NavigationScreen.PROFILE
                                showViewShiftDetails = true
                            }
                            is DeepLink.PostShiftCheck -> {
                                val parsed = parsePostShiftDataJson(deepLink.postShiftData)
                                    ?: NotificationEventBus.pendingPostShiftPayload
                                    ?: parsePostShiftDataJson(preferencesManager.postShiftDataJson)
                                if (parsed != null) {
                                    Log.d(TAG, "Showing post-shift dialog from local data (recordId: ${parsed.attendanceRecordId})")
                                    postShiftPayload = parsed
                                    postShiftLoading = false
                                    postShiftResult = null
                                    postShiftIsError = false
                                } else {
                                    // No local data — fetch from server
                                    Log.d(TAG, "No local post-shift data, checking server...")
                                    scope.launch {
                                        fetchPostShiftFromServer(
                                            token = preferencesManager.authToken.orEmpty(),
                                            onResult = { payload ->
                                                postShiftPayload = payload
                                                postShiftLoading = false
                                                postShiftResult = null
                                                postShiftIsError = false
                                            },
                                            onNone = {
                                                Log.d(TAG, "Server has no pending post-shift check")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            // ATTENDANCE DAY DETAIL SCREEN
            selectedAttendanceDate != null -> {
                val attendanceHistoryViewModel: AttendanceHistoryViewModel = koinViewModel()

                AttendanceDayDetailScreen(
                    date = selectedAttendanceDate!!,
                    viewModel = attendanceHistoryViewModel,
                    onNavigateBack = {
                        selectedAttendanceDate = null
                        showAttendanceHistory = false
                        currentNavigationScreen = NavigationScreen.ATTENDANCE
                    },
                    bottomPadding = paddingValues.calculateBottomPadding()
                )
            }

            // Leave Detail Screen
            showLeaveDetail != null -> {
                LeaveDetailScreen(
                    applicationId = showLeaveDetail!!,
                    onNavigateBack = {
                        showLeaveDetail = null
                        showLeaveHistory = true
                    }
                )
            }

            // Leave History Screen
            showLeaveHistory -> {
                val leaveViewModel: LeaveViewModel = koinViewModel()

                LeaveHistoryScreen(
                    viewModel = leaveViewModel,
                    onNavigateBack = {
                        showLeaveHistory = false
                        currentNavigationScreen = NavigationScreen.HOME
                    },
                    onNavigateToApplyLeave = {
                        showLeaveHistory = false
                        showApplyLeave = true
                    },
                    onNavigateToDetail = { applicationId ->
                        showLeaveHistory = false
                        showLeaveDetail = applicationId
                    },
                    successMessage = leaveSubmitSuccessMessage,
                    onSuccessMessageShown = {
                        leaveSubmitSuccessMessage = null
                    }
                )
            }

            // Apply Leave Screen
            showApplyLeave -> {
                val leaveViewModel: LeaveViewModel = koinViewModel()

                ApplyLeaveScreen(
                    viewModel = leaveViewModel,
                    onNavigateBack = {
                        showApplyLeave = false
                        showLeaveHistory = true  // Go back to Leave History instead of Home
                    },
                    onNavigateToHistory = { success ->
                        showApplyLeave = false
                        showLeaveHistory = true
                        if (success == 1) {
                            leaveSubmitSuccessMessage = "Leave application submitted successfully!"
                        }
                    }
                )
            }

            // Circular Detail Screen
            showCircularDetail != null -> {
                val circularViewModel: CircularViewModel = koinViewModel()

                CircularDetailScreen(
                    viewModel = circularViewModel,
                    circularId = showCircularDetail!!,
                    onNavigateBack = {
                        showCircularDetail = null
                        // If came from circular list, go back to list
                        // If came from notification, go to home
                        if (showCircularList) {
                            // Stay on circular list logic already handled
                        }
                        showCircularList = true
                    }
                )
            }

            // Circular List Screen
            showCircularList -> {
                val circularViewModel: CircularViewModel = koinViewModel()

                CircularListScreen(
                    viewModel = circularViewModel,
                    onNavigateToDetail = { circularId ->
                        showCircularDetail = circularId
                        showCircularList = false
                    },
                    onNavigateBack = {
                        showCircularList = false
                        currentNavigationScreen = NavigationScreen.HOME
                    }
                )
            }

            showEditSocialMedia -> {
                EditSocialMediaScreen(
                    onBack = {
                        showEditSocialMedia = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditContactDetail -> {
                EditContactDetailScreen(
                    onBack = {
                        showEditContactDetail = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditPersonalInfo -> {
                EditPersonalInfoScreen(
                    onBack = {
                        showEditPersonalInfo = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewEmploymentDetail -> {
                ViewEmploymentDetailScreen(
                    onBack = {
                        showViewEmploymentDetail = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showEditBankingInfo -> {
                EditBankingInfoScreen(
                    onBack = {
                        showEditBankingInfo = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewEmploymentIdentity -> {
                ViewEmploymentIdentityScreen(
                    onBack = {
                        showViewEmploymentIdentity = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            showViewShiftDetails -> {
                ViewShiftDetailsScreen(
                    onBack = {
                        showViewShiftDetails = false
                        currentNavigationScreen = NavigationScreen.PROFILE
                    }
                )
            }

            // WORK REPORT SCREEN
            showWorkReport -> {
                val workReportViewModel: WorkReportViewModel = koinViewModel()

                WorkReportScreen(
                    viewModel = workReportViewModel,
                    onNavigateBack = {
                        showWorkReport = false
                        currentNavigationScreen = NavigationScreen.HOME
                    },
                    bottomPadding = paddingValues.calculateBottomPadding()
                )
            }

            else -> {
                // Main navigation screens
                when (currentNavigationScreen) {
                    NavigationScreen.HOME -> {
                        HomePage(
                            onLogout = onLogout,
                            onNavigateToProfile = {
                                currentNavigationScreen = NavigationScreen.PROFILE
                            },
                            onNavigateToWorkReport = {
                                showWorkReport = true
                            },
                            onNavigateToCircular = {
                                showCircularList = true
                            },
                            onNavigateToApplyLeaves = {
                                showLeaveHistory = true  // ✅ FIXED: Show overview first instead of form
                            },
                            onNavigateToNotifications = {
                                showNotificationList = true
                            },
                            onNavigateToAttendanceHistory = {
                                currentNavigationScreen = NavigationScreen.ATTENDANCE
                            },
                            paddingValues = paddingValues
                        )
                    }

                    NavigationScreen.ATTENDANCE -> {
                        val attendanceHistoryViewModel: AttendanceHistoryViewModel = koinViewModel()

                        AttendanceHistoryScreen(
                            viewModel = attendanceHistoryViewModel,
                            onNavigateBack = {
                                currentNavigationScreen = NavigationScreen.HOME
                            },
                            onNavigateToDayDetail = { date ->
                                selectedAttendanceDate = date
                                showAttendanceHistory = true
                            },
                            paddingValues = paddingValues
                        )
                    }

                    NavigationScreen.PROFILE -> {
                        ProfileScreen(
                            onNavigateToFaceChange = {
                                // No action needed - ProfileScreen handles it internally
                            },
                            onNavigateToEditSocialMedia = {
                                showEditSocialMedia = true
                            },
                            onNavigateToEditContactDetail = {
                                showEditContactDetail = true
                            },
                            onNavigateToEditPersonalInfo = {
                                showEditPersonalInfo = true
                            },
                            onNavigateToViewEmploymentDetail = {
                                showViewEmploymentDetail = true
                            },
                            onNavigateToEditBankingInfo = {
                                showEditBankingInfo = true
                            },
                            onNavigateToViewEmploymentIdentity = {
                                showViewEmploymentIdentity = true
                            },
                            onNavigateToViewShiftDetails = {
                                showViewShiftDetails = true
                            },
                            onLogout = onLogout,
                            onBack = {
                                currentNavigationScreen = NavigationScreen.HOME
                            }
                        )
                    }
                }
            }
        }
    }

    // Post-shift check dialog (rendered as overlay above everything)
    if (postShiftPayload != null) {
        PostShiftCheckDialog(
            payload = postShiftPayload!!,
            isLoading = postShiftLoading,
            resultMessage = postShiftResult,
            isError = postShiftIsError,
            onAction = { actionId ->
                if (postShiftLoading) return@PostShiftCheckDialog
                val payload = postShiftPayload ?: return@PostShiftCheckDialog
                postShiftLoading = true
                postShiftIsError = false
                scope.launch {
                    try {
                        val apiService = NetworkModule.apiService
                        val token = preferencesManager.authToken.orEmpty()
                        val response = apiService.postShiftResponse(
                            body = PostShiftRequestBody(
                                attendanceRecordId = payload.attendanceRecordId,
                                working = actionId == "working"
                            ),
                            token = token
                        )
                        val body = response.body()
                        if (response.isSuccessful && body?.success == true) {
                            postShiftResult = body.message ?: "Response submitted"
                            postShiftLoading = false
                            postShiftIsError = false
                            NotificationEventBus.clearPendingPostShiftPayload()
                            preferencesManager.clearPostShiftData()
                            NotificationEventBus.notifyAttendanceRefresh()
                            // Stop location tracking if employee is done working
                            if (actionId == "done") {
                                LocationTrackingService.stopTracking(context)
                            }
                            // Auto-dismiss after 1.5s
                            delay(1500)
                            postShiftPayload = null
                            postShiftResult = null
                        } else {
                            val errorMsg = response.body()?.message
                                ?: response.errorBody()?.string()?.let {
                                    try { JSONObject(it).optString("message", "") } catch (_: Exception) { "" }
                                }?.ifEmpty { null }
                                ?: "Failed to submit response"
                            postShiftResult = errorMsg
                            postShiftLoading = false
                            postShiftIsError = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Post-shift response failed", e)
                        postShiftResult = e.message ?: "Network error"
                        postShiftLoading = false
                        postShiftIsError = true
                    }
                }
            },
            onDismiss = {
                postShiftPayload = null
                postShiftLoading = false
                postShiftResult = null
                postShiftIsError = false
            }
        )
    }
}

/**
 * Parse post_shift_data JSON string into PostShiftCheckPayload
 */
private fun parsePostShiftDataJson(json: String?): PostShiftCheckPayload? {
    if (json.isNullOrBlank()) return null
    return try {
        val obj = JSONObject(json)
        val attendanceRecordId = obj.optInt("attendance_record_id", 0)
        if (attendanceRecordId == 0) return null

        val actions = mutableListOf<PostShiftAction>()
        val actionsArr = obj.optJSONArray("actions")
        if (actionsArr != null) {
            for (i in 0 until actionsArr.length()) {
                val actionObj = actionsArr.getJSONObject(i)
                actions.add(
                    PostShiftAction(
                        id = actionObj.optString("id", ""),
                        label = actionObj.optString("label", "")
                    )
                )
            }
        }
        if (actions.isEmpty()) {
            actions.add(PostShiftAction("working", "Yes, Working"))
            actions.add(PostShiftAction("done", "No, Done"))
        }

        PostShiftCheckPayload(
            attendanceRecordId = attendanceRecordId,
            attendanceDate = obj.optString("attendance_date", "").ifEmpty { null },
            employeeId = obj.optInt("employee_id", 0).takeIf { it > 0 },
            shiftEndTime = obj.optString("shift_end_time", "").ifEmpty { null },
            actions = actions,
            title = obj.optString("title", "Shift Check"),
            message = obj.optString("message", "Are you still working?")
        )
    } catch (e: Exception) {
        Log.e("MainScreen", "Failed to parse post_shift_data JSON", e)
        null
    }
}

/**
 * Fetch pending post-shift check from server.
 * Calls GET /post-shift-check — the single source of truth.
 */
private suspend fun fetchPostShiftFromServer(
    token: String,
    attendanceRecordId: Int? = null,
    onResult: (PostShiftCheckPayload) -> Unit,
    onNone: () -> Unit
) {
    try {
        val res = NetworkModule.apiService.getPostShiftStatus(token, attendanceRecordId)
        val body = res.body()
        if (res.isSuccessful && body?.success == true && body.data != null) {
            val d = body.data
            val actions = d.actions?.takeIf { it.isNotEmpty() }
                ?: listOf(
                    PostShiftAction("working", "Yes, Working"),
                    PostShiftAction("done", "No, Done")
                )
            val payload = PostShiftCheckPayload(
                attendanceRecordId = d.attendance_record_id,
                attendanceDate = d.attendance_date,
                employeeId = d.employee_id,
                shiftEndTime = d.shift_end_time,
                actions = actions,
                title = d.title ?: "Shift Check",
                message = d.message ?: "Are you still working?"
            )
            Log.d("MainScreen", "Post-shift data from server (recordId: ${d.attendance_record_id})")
            onResult(payload)
        } else {
            onNone()
        }
    } catch (e: Exception) {
        Log.e("MainScreen", "Failed to fetch post-shift status from server", e)
    }
}