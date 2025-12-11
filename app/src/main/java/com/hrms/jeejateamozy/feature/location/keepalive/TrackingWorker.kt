package com.hrms.jeejateamozy.feature.location.keepalive

import android.content.Context
import android.util.Log
import androidx.work.*
import com.hrms.jeejateamozy.feature.location.heartbeat.TrackingStateManager
import com.hrms.jeejateamozy.feature.location.service.LocationTrackingService
import java.util.concurrent.TimeUnit

/**
 * WorkManager keep-alive (every 15 minutes)
 */
class TrackingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TrackingWorker"
        private const val WORK_NAME = "location_tracking_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<TrackingWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            Log.d(TAG, "✅ WorkManager scheduled")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "⏹️ WorkManager cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "🔄 WorkManager check triggered")

        val stateManager = TrackingStateManager(applicationContext)

        if (stateManager.shouldTrackingBeActive()) {
            Log.d(TAG, "✅ Tracking should be active - ensuring service is running")
            LocationTrackingService.startTracking(applicationContext)
            return Result.success()
        } else {
            Log.d(TAG, "⏹️ Tracking should NOT be active")
            return Result.success()
        }
    }
}