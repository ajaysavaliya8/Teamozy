package com.hrms.jeejateamozy.feature.location.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.hrms.jeejateamozy.R
import com.hrms.jeejateamozy.app.MainActivity
import com.hrms.jeejateamozy.feature.location.tracking.LocationTracker
import com.hrms.jeejateamozy.feature.location.sync.SyncManager
import com.hrms.jeejateamozy.feature.location.sync.SyncResult
import com.hrms.jeejateamozy.feature.location.data.LocationData
import com.hrms.jeejateamozy.feature.location.data.LocationSyncRequest
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.*

/**
 * Foreground Service for continuous location tracking
 * Runs from check-in to check-out
 */
class LocationTrackingService : Service() {

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 12345

        // GPS Settings
        private const val LOCATION_UPDATE_INTERVAL = 5000L        // 5 seconds
        private const val LOCATION_FASTEST_INTERVAL = 3000L       // 3 seconds

        // Service actions
        const val ACTION_START_TRACKING = "com.hrms.jeejateamozy.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.hrms.jeejateamozy.STOP_TRACKING"

        /**
         * Start location tracking service
         */
        fun startTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START_TRACKING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop location tracking service
         */
        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTracker: LocationTracker
    private lateinit var syncManager: SyncManager
    private lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                Log.d(TAG, "GPS Update: (${location.latitude}, ${location.longitude}), " +
                        "Speed: ${String.format("%.2f", location.speed * 3.6f)} km/h, " +
                        "Accuracy: ${String.format("%.1f", location.accuracy)}m")

                // Pass to location tracker
                locationTracker.processLocation(location)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        preferencesManager = PreferencesManager.getInstance(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create notification channel
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_TRACKING -> {
                startForegroundTracking()
            }
            ACTION_STOP_TRACKING -> {
                stopForegroundTracking()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        cleanup()
    }

    /**
     * Start foreground tracking
     */
    private fun startForegroundTracking() {
        try {
            // Start foreground service with notification
            val notification = buildNotification()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // Initialize components
            initializeTracking()

            // Start location updates
            startLocationUpdates()

            Log.d(TAG, "✅ Foreground tracking started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground tracking", e)
            stopSelf()
        }
    }

    /**
     * Initialize location tracker and sync manager
     */
    private fun initializeTracking() {
        val deviceId = preferencesManager.getDeviceId() ?: "unknown"
        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }

        // Initialize location tracker
        locationTracker = LocationTracker(
            context = applicationContext,
            deviceId = deviceId,
            appVersion = appVersion,
            onLocationRecorded = { locationData ->
                // Add to sync manager
                syncManager.addLocation(locationData)
            }
        )

        // Initialize sync manager
        syncManager = SyncManager(
            context = applicationContext,
            onSync = { locations -> syncLocations(locations) },
            onTrackingStop = {
                // 403 received - stop tracking
                Log.d(TAG, "Stopping tracking due to no active session")
                stopForegroundTracking()
            }
        )

        // Start periodic sync checker
        syncManager.startPeriodicChecker()

        Log.d(TAG, "Tracking components initialized")
    }

    /**
     * Start receiving GPS location updates
     */
    private fun startLocationUpdates() {
        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing location permission")
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(LOCATION_UPDATE_INTERVAL * 2)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "Location updates started (interval: ${LOCATION_UPDATE_INTERVAL}ms)")
    }

    /**
     * Stop foreground tracking and cleanup
     */
    private fun stopForegroundTracking() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Stopping tracking...")

                // Stop location updates
                fusedLocationClient.removeLocationUpdates(locationCallback)

                // Force final sync
                if (::syncManager.isInitialized) {
                    Log.d(TAG, "Performing final sync...")
                    syncManager.forceSyncAll()
                }

                // Cleanup
                cleanup()

                // Stop service
                stopForeground(true)
                stopSelf()

                Log.d(TAG, "✅ Tracking stopped successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping tracking", e)
            }
        }
    }

    /**
     * Sync locations to backend API
     */
    private suspend fun syncLocations(locations: List<LocationData>): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesManager.getAuthToken()
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "No auth token available")
                    return@withContext SyncResult.Error("No auth token")
                }

                val request = LocationSyncRequest(locations)

                val response = NetworkModule.apiService.syncLocationTracking(
                    locations = locations.map { locationToMap(it) }
                )

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val body = response.body()
                        if (body?.status == "success") {
                            Log.d(TAG, "✅ API sync success")
                            SyncResult.Success
                        } else {
                            Log.e(TAG, "API returned non-success status")
                            SyncResult.Error("API error")
                        }
                    }

                    response.code() == 403 -> {
                        Log.e(TAG, "❌ 403: No active session")
                        SyncResult.NoActiveSession
                    }

                    else -> {
                        val errorMsg = "API error: ${response.code()}"
                        Log.e(TAG, errorMsg)
                        SyncResult.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during sync", e)
                SyncResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Convert LocationData to Map for API
     */
    private fun locationToMap(location: LocationData): Map<String, Any?> {
        return mapOf(
            "recorded_at" to location.recordedAt,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "location_accuracy" to location.locationAccuracy,
            "altitude" to (location.altitude ?: 0.0),
            "vertical_accuracy" to location.verticalAccuracy,
            "speed" to location.speed,
            "heading" to location.heading,
            "device_id" to location.deviceId,
            "app_version" to location.appVersion,
            "network_type" to location.networkType,
            "wifi_name" to location.wifiName,
            "wifi_mac_address" to location.wifiMacAddress,
            "battery_level" to location.batteryLevel,
            "geofence_id" to location.geofenceId
        )
    }

    /**
     * Create notification channel (Android O+)
     */
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

    /**
     * Build foreground service notification
     */
    private fun buildNotification(): Notification {
        // Intent to open app when notification is clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Teamozy - Tracking Attendance")
            .setContentText("Location tracking is active")
            .setSmallIcon(R.drawable.ic_notification)  // You'll need to add this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)

            if (::syncManager.isInitialized) {
                syncManager.cleanup()
            }

            if (::locationTracker.isInitialized) {
                locationTracker.reset()
            }

            serviceScope.cancel()

            Log.d(TAG, "Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}