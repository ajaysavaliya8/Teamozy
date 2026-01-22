package com.hrms.jeejateamozy.feature.location.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hrms.jeejateamozy.BuildConfig
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.app.MainActivity
import com.hrms.jeejateamozy.feature.location.data.local.PendingLocationEntity
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.location.data.repository.LocationRepository
import com.hrms.jeejateamozy.feature.location.data.repository.SyncResult
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingAlarmReceiver
import com.hrms.jeejateamozy.feature.location.keepalive.TrackingWorker
import com.hrms.jeejateamozy.feature.location.util.GpsStatusHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Foreground service for continuous location tracking
 *
 * Features:
 * - GPS location capture every 30 seconds
 * - Automatic sync every 60 seconds
 * - GPS status monitoring (detects when user disables GPS)
 * - Records GPS disabled events to server
 *
 * Lifecycle:
 * - START: After check-in signature complete
 * - STOP: After check-out signature OR 403 from sync API
 */
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"

        // Notification
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001

        // Intent extras
        private const val EXTRA_GEOFENCE_ID = "geofence_id"
        private const val EXTRA_COMMAND = "command"
        private const val COMMAND_START = "start"
        private const val COMMAND_STOP = "stop"

        // Broadcast action for UI refresh (when stop_tracking is received)
        const val ACTION_REFRESH_CHECK_STATUS = "com.hrms.jeejateamozy.ACTION_REFRESH_CHECK_STATUS"

        // Timing - TESTING VALUES (reduce for production)
        private const val LOCATION_INTERVAL_MS = 30_000L      // 30 seconds (testing)
        private const val LOCATION_FASTEST_MS = 15_000L       // 15 seconds minimum
        private const val SYNC_INTERVAL_MS = 60_000L          // 1 minute (testing) - change to 5 min for production
        private const val SYNC_THRESHOLD_COUNT = 10           // Sync when 10+ locations pending (testing)

//        private const val LOCATION_INTERVAL_MS = 60_000L      // 60 seconds
//        private const val SYNC_INTERVAL_MS = 300_000L         // 5 minutes
//        private const val SYNC_THRESHOLD_COUNT = 50           // 50 locations

        // GPS disabled check interval
        private const val GPS_CHECK_INTERVAL_MS = 30_000L     // Check every 30 seconds when GPS is disabled

        /**
         * Start location tracking service
         */
        fun startTracking(context: Context, geofenceId: Int? = null) {
            Log.d(TAG, "▶️ startTracking() called - geofenceId=$geofenceId")

            val intent = Intent(context, LocationTrackingService::class.java).apply {
                putExtra(EXTRA_COMMAND, COMMAND_START)
                geofenceId?.let { putExtra(EXTRA_GEOFENCE_ID, it) }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start service", e)
            }
        }

        /**
         * Stop location tracking service
         */
        fun stopTracking(context: Context) {
            Log.d(TAG, "⏹️ stopTracking() called")

            try {
                val intent = Intent(context, LocationTrackingService::class.java).apply {
                    putExtra(EXTRA_COMMAND, COMMAND_STOP)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to stop service", e)
            }
        }
    }

    // Dependencies
    private val locationRepository: LocationRepository by inject()
    private val apiService: ApiService by inject()

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // GPS Status monitoring
    private var gpsStatusHelper: GpsStatusHelper? = null
    private var isGpsEnabled = true
    private var gpsDisabledJob: Job? = null
    private var lastGpsDisabledRecordTime = 0L

    // Coroutines
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    // State
    private var geofenceId: Int? = null
    private var isTracking = false
    private var wakeLock: PowerManager.WakeLock? = null

    // ==========================================
    // SERVICE LIFECYCLE
    // ==========================================

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "📍 Service onCreate()")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        setupLocationCallback()
        setupGpsStatusMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📍 Service onStartCommand() - command=${intent?.getStringExtra(EXTRA_COMMAND)}")

        when (intent?.getStringExtra(EXTRA_COMMAND)) {
            COMMAND_START -> {
                geofenceId = intent.getIntExtra(EXTRA_GEOFENCE_ID, -1).takeIf { it != -1 }
                startForegroundTracking()
            }
            COMMAND_STOP -> {
                stopForegroundTracking()
            }
            else -> {
                // Service restarted by system - check if should be tracking
                val stateManager = TrackingStateManager(this)
                if (stateManager.shouldTrackingBeActive()) {
                    Log.d(TAG, "🔄 Service restarted - resuming tracking")
                    startForegroundTracking()
                } else {
                    Log.d(TAG, "⏹️ Service restarted but tracking not active - stopping")
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "📍 Service onDestroy()")
        stopLocationUpdates()
        stopGpsStatusMonitoring()
        syncJob?.cancel()
        gpsDisabledJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    // ==========================================
    // GPS STATUS MONITORING
    // ==========================================

    private fun setupGpsStatusMonitoring() {
        gpsStatusHelper = GpsStatusHelper(this) { enabled ->
            handleGpsStatusChange(enabled)
        }
    }

    private fun startGpsStatusMonitoring() {
        gpsStatusHelper?.let {
            isGpsEnabled = it.isGpsEnabled()
            it.startMonitoring()

            if (!isGpsEnabled) {
                Log.w(TAG, "⚠️ GPS is already disabled at start!")
                handleGpsDisabled()
            }
        }
    }

    private fun stopGpsStatusMonitoring() {
        gpsStatusHelper?.stopMonitoring()
        gpsDisabledJob?.cancel()
    }

    private fun handleGpsStatusChange(enabled: Boolean) {
        isGpsEnabled = enabled

        if (enabled) {
            handleGpsEnabled()
        } else {
            handleGpsDisabled()
        }
    }

    private fun handleGpsDisabled() {
        Log.w(TAG, "🚫 GPS DISABLED by user!")

        // Update notification to warn user
        updateNotification(
            title = "⚠️ GPS Disabled",
            text = "Please enable GPS for location tracking"
        )

        // Record GPS disabled event immediately
        recordGpsDisabledEvent()

        // Start periodic recording of GPS disabled state
        startGpsDisabledRecording()
    }

    private fun handleGpsEnabled() {
        Log.d(TAG, "✅ GPS ENABLED by user")

        // Stop GPS disabled recording
        gpsDisabledJob?.cancel()
        gpsDisabledJob = null

        // Restore normal notification
        updateNotification(
            title = "Location Tracking Active",
            text = "Your location is being recorded during work hours"
        )

        // Resume location updates (they should auto-resume, but ensure)
        if (isTracking) {
            startLocationUpdates()
        }
    }

    private fun startGpsDisabledRecording() {
        gpsDisabledJob?.cancel()

        gpsDisabledJob = serviceScope.launch {
            while (isActive && !isGpsEnabled && isTracking) {
                delay(GPS_CHECK_INTERVAL_MS)

                if (!isGpsEnabled && isTracking) {
                    recordGpsDisabledEvent()
                }
            }
        }
    }

    private fun recordGpsDisabledEvent() {
        val now = System.currentTimeMillis()

        // Don't record too frequently (at least 30 seconds apart)
        if (now - lastGpsDisabledRecordTime < GPS_CHECK_INTERVAL_MS) {
            return
        }
        lastGpsDisabledRecordTime = now

        serviceScope.launch {
            try {
                // Store a special location entry with 0,0 coordinates and a flag
                val entity = PendingLocationEntity(
                    recordedAt = getCurrentISOTimestamp(),
                    latitude = 0.0,           // Indicates GPS disabled
                    longitude = 0.0,          // Indicates GPS disabled
                    locationAccuracy = -1f,   // -1 indicates GPS disabled
                    altitude = 0.0,
                    verticalAccuracy = null,
                    speed = null,
                    heading = null,
                    deviceId = getDeviceIdentifier(),
                    appVersion = BuildConfig.VERSION_NAME,
                    networkType = getNetworkType(),
                    wifiName = getWifiName(),
                    wifiMacAddress = getWifiMacAddress(),
                    batteryLevel = getBatteryLevel(),
                    geofenceId = geofenceId
                )

                val success = locationRepository.storeLocation(entity)
                if (success) {
                    Log.w(TAG, "📍 GPS DISABLED event recorded")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to record GPS disabled event", e)
            }
        }
    }

    // ==========================================
    // TRACKING CONTROL
    // ==========================================

    private fun startForegroundTracking() {
        if (isTracking) {
            Log.d(TAG, "⚠️ Already tracking - ignoring start request")
            return
        }

        Log.d(TAG, "▶️ Starting foreground tracking - geofenceId=$geofenceId")

        // Start foreground with notification FIRST (required before any other work)
        startForeground(NOTIFICATION_ID, createNotification())

        // Acquire wake lock (with try-catch for safety)
        acquireWakeLock()

        // Start GPS status monitoring
        startGpsStatusMonitoring()

        // Start location updates
        startLocationUpdates()

        // Start periodic sync
        startPeriodicSync()

        isTracking = true

        Log.d(TAG, "✅ Foreground tracking started")
    }

    private fun stopForegroundTracking() {
        Log.d(TAG, "⏹️ Stopping foreground tracking")

        isTracking = false

        // Stop location updates
        stopLocationUpdates()

        // Stop GPS monitoring
        stopGpsStatusMonitoring()

        // Cancel sync job
        syncJob?.cancel()

        // Release wake lock
        releaseWakeLock()

        // Stop foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        // Stop service
        stopSelf()

        Log.d(TAG, "✅ Foreground tracking stopped")
    }

    // ==========================================
    // LOCATION UPDATES
    // ==========================================

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d(TAG, "📍 Location received: lat=${location.latitude}, lng=${location.longitude}, acc=${location.accuracy}m")

                    serviceScope.launch {
                        saveLocation(location)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        Log.d(TAG, "🔄 Starting location updates - interval=${LOCATION_INTERVAL_MS}ms")

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_MS)
            .setWaitForAccurateLocation(false)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
            Log.d(TAG, "✅ Location updates started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start location updates", e)
        }
    }

    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d(TAG, "✅ Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop location updates", e)
        }
    }

    // ==========================================
    // LOCATION STORAGE
    // ==========================================

    private suspend fun saveLocation(location: android.location.Location) {
        try {
            val entity = PendingLocationEntity(
                recordedAt = getCurrentISOTimestamp(),
                latitude = location.latitude,
                longitude = location.longitude,
                locationAccuracy = location.accuracy,
                altitude = 0.0,  // Always 0 as per API spec
                verticalAccuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && location.hasVerticalAccuracy()) {
                    location.verticalAccuracyMeters
                } else null,
                speed = if (location.hasSpeed()) location.speed else null,
                heading = if (location.hasBearing()) location.bearing else null,
                deviceId = getDeviceIdentifier(),
                appVersion = BuildConfig.VERSION_NAME,
                networkType = getNetworkType(),
                wifiName = getWifiName(),
                wifiMacAddress = getWifiMacAddress(),
                batteryLevel = getBatteryLevel(),
                geofenceId = geofenceId
            )

            val success = locationRepository.storeLocation(entity)

            if (success) {
                val pendingCount = locationRepository.getPendingCount()
                Log.d(TAG, "📍 Location saved - $pendingCount pending")

                // Check if threshold reached for immediate sync
                if (pendingCount >= SYNC_THRESHOLD_COUNT) {
                    Log.d(TAG, "📤 Threshold reached - triggering sync")
                    performSync()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save location", e)
        }
    }

    // ==========================================
    // PERIODIC SYNC
    // ==========================================

    private fun startPeriodicSync() {
        syncJob?.cancel()

        syncJob = serviceScope.launch {
            Log.d(TAG, "🔄 Starting periodic sync - interval=${SYNC_INTERVAL_MS}ms")

            while (isActive && isTracking) {
                delay(SYNC_INTERVAL_MS)

                if (isTracking) {
                    performSync()
                }
            }
        }
    }

    private suspend fun performSync() {
        Log.d(TAG, "🔄 Performing sync...")

        try {
            when (val result = locationRepository.syncPendingLocations()) {
                is SyncResult.Success -> {
                    Log.d(TAG, "✅ Sync complete - ${result.synced} synced, ${result.remaining} remaining")
                }

                is SyncResult.SessionEnded -> {
                    Log.w(TAG, "⚠️ 403 received - session ended, stopping service")
                    handleSessionEnded()
                }

                is SyncResult.StopTracking -> {
                    Log.w(TAG, "⚠️ stop_tracking=true received - stopping service and refreshing status")
                    handleStopTracking()
                }

                is SyncResult.NetworkError -> {
                    Log.e(TAG, "🌐 Network error - will retry next cycle: ${result.message}")
                }

                is SyncResult.Error -> {
                    Log.e(TAG, "❌ Sync error - will retry next cycle: ${result.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync exception", e)
        }
    }

    /**
     * Handle 403 response - session ended server-side
     */
    private fun handleSessionEnded() {
        Log.w(TAG, "⚠️ Handling session ended (403)")

        serviceScope.launch {
            try {
                // Clear all pending locations
                locationRepository.clearAllLocations()
                Log.d(TAG, "🗑️ Pending locations cleared")

                // Clear tracking state
                val stateManager = TrackingStateManager(this@LocationTrackingService)
                stateManager.clearState()

                // Cancel keep-alive mechanisms
                TrackingWorker.cancel(this@LocationTrackingService)
                TrackingAlarmReceiver.cancel(this@LocationTrackingService)

                // Stop service
                stopForegroundTracking()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling session ended", e)
            }
        }
    }

    /**
     * Handle stop_tracking=true response - API signals to stop tracking and refresh status
     */
    private fun handleStopTracking() {
        Log.w(TAG, "⚠️ Handling stop_tracking (400 with stop_tracking=true)")

        serviceScope.launch {
            try {
                // 1. Clear all pending locations
                locationRepository.clearAllLocations()
                Log.d(TAG, "🗑️ Pending locations cleared")

                // 2. Call check-status API to refresh check-out status
                refreshCheckStatus()

                // 3. Clear tracking state
                val stateManager = TrackingStateManager(this@LocationTrackingService)
                stateManager.clearState()

                // 4. Cancel keep-alive mechanisms
                TrackingWorker.cancel(this@LocationTrackingService)
                TrackingAlarmReceiver.cancel(this@LocationTrackingService)

                // 5. Stop service
                stopForegroundTracking()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error handling stop tracking", e)
                // Still stop the service even if check-status fails
                stopForegroundTracking()
            }
        }
    }

    /**
     * Call check-status API to refresh the current attendance status
     */
    private suspend fun refreshCheckStatus() {
        try {
            val preferencesManager = PreferencesManager.getInstance(this@LocationTrackingService)
            val deviceId = preferencesManager.deviceId
            val token = preferencesManager.authToken

            if (deviceId.isBlank() || token.isNullOrBlank()) {
                Log.w(TAG, "⚠️ Missing deviceId or token for check-status")
                return
            }

            Log.d(TAG, "📡 Calling check-status API to refresh attendance status...")
            val response = apiService.checkStatus(deviceId, token)

            if (response.isSuccessful) {
                val statusData = response.body()
                Log.d(TAG, "✅ Check-status refreshed - state: ${statusData?.data?.current_state}")

                // Broadcast to notify UI to refresh
                val refreshIntent = Intent(ACTION_REFRESH_CHECK_STATUS)
                sendBroadcast(refreshIntent)
            } else {
                Log.e(TAG, "❌ Check-status failed - HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling check-status", e)
        }
    }

    // ==========================================
    // NOTIFICATION
    // ==========================================

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your location during work hours"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(
        title: String = "Location Tracking Active",
        text: String = "Your location is being recorded during work hours"
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = createNotification(title, text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    // ==========================================
    // WAKE LOCK (with safe error handling)
    // ==========================================

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Teamozy:LocationTracking"
                )
                wakeLock?.acquire()
                Log.d(TAG, "🔋 Wake lock acquired")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "⚠️ WAKE_LOCK permission not granted - continuing without wake lock", e)
            // Continue without wake lock - service will still work but may be less reliable
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to acquire wake lock", e)
            wakeLock = null
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "🔋 Wake lock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error releasing wake lock", e)
        }
        wakeLock = null
    }

    // ==========================================
    // DEVICE INFO HELPERS
    // ==========================================

    private fun getCurrentISOTimestamp(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT)
        } else {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(java.util.Date())
        }
    }

    private fun getDeviceIdentifier(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: ""
    }

    private fun getBatteryLevel(): Int? {
        return try {
            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) (level * 100 / scale) else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            null
        }
    }

    private fun getNetworkType(): String? {
        return try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return "OFFLINE"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "OFFLINE"

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                else -> "OTHER"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getWifiName(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo?.ssid?.replace("\"", "")?.takeIf { it != "<unknown ssid>" }
        } catch (e: SecurityException) {
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi name", e)
            null
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getWifiMacAddress(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            wifiInfo?.bssid?.takeIf { it != "02:00:00:00:00:00" }
        } catch (e: SecurityException) {
            Log.w(TAG, "ACCESS_WIFI_STATE permission not granted")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting WiFi MAC", e)
            null
        }
    }
}