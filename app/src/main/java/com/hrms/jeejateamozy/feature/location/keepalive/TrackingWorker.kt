package com.hrms.jeejateamozy.feature.location.keepalive

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import androidx.work.*
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker - Periodic Check for Tracking Service
 *
 * Runs every 15 minutes to check if location tracking should be active
 * If service is dead but should be running, automatically restarts it
 *
 * Advantages:
 * - Survives app kills
 * - Survives device restarts
 * - Battery efficient
 * - Guaranteed execution (may be delayed)
 */
class TrackingWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TrackingWorker"
        private const val WORK_NAME = "tracking_check_worker"

        /**
         * Schedule periodic tracking check
         * Call this on check-in
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)  // Works offline
                .setRequiresBatteryNotLow(false)  // Works even if battery low
                .build()

            val workRequest = PeriodicWorkRequestBuilder<TrackingWorker>(
                15, TimeUnit.MINUTES  // Minimum interval for periodic work
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,  // Don't restart if already running
                    workRequest
                )

            Log.d(TAG, "✅ WorkManager scheduled - periodic check every 15 minutes")
        }

        /**
         * Cancel periodic tracking check
         * Call this on check-out
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "⏹️ WorkManager cancelled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔍 WorkManager: Checking tracking status...")

            val trackingStateManager = TrackingStateManager(applicationContext)

            // Check if tracking should be active
            val shouldBeActive = trackingStateManager.shouldTrackingBeActive()

            if (shouldBeActive) {
                // Check if service is actually running
                val isRunning = isLocationServiceRunning()

                if (!isRunning) {
                    Log.w(TAG, "⚠️ Tracking should be active but service is NOT running!")
                    Log.d(TAG, "🔄 Attempting to restart LocationTrackingService...")

                    // Restart service
                    LocationTrackingService.startTracking(applicationContext)

                    Log.d(TAG, "✅ Service restart triggered by WorkManager")
                } else {
                    Log.d(TAG, "✅ Service is running correctly")
                }
            } else {
                Log.d(TAG, "⏹️ Tracking should NOT be active - no action needed")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in WorkManager worker", e)
            Result.retry()  // Retry on error
        }
    }

    /**
     * Check if LocationTrackingService is running
     */
    private fun isLocationServiceRunning(): Boolean {
        return try {
            val manager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            manager.getRunningServices(Int.MAX_VALUE).any { service ->
                service.service.className == LocationTrackingService::class.java.name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status", e)
            false
        }
    }
}