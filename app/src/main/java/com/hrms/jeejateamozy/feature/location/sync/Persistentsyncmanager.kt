package com.hrms.jeejateamozy.feature.location.sync

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.feature.location.database.PendingLocationDao
import com.hrms.jeejateamozy.feature.location.database.PendingLocationEntity
import com.hrms.jeejateamozy.feature.location.database.LocationDatabase
import com.hrms.jeejateamozy.feature.location.model.LocationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException

class PersistentSyncManager private constructor(
    private val dao: PendingLocationDao,
    private val context: Context
) : KoinComponent {

    private val apiService: ApiService by inject()

    companion object {
        private const val TAG = "PersistentSyncManager"
        private const val OPTIMAL_BATCH_SIZE = 15
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 5_000L

        @Volatile
        private var instance: PersistentSyncManager? = null

        fun getInstance(context: Context): PersistentSyncManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val database = LocationDatabase.getInstance(context)
                    PersistentSyncManager(
                        dao = database.pendingLocationDao(),
                        context = context.applicationContext
                    ).also { instance = it }
                }
            }
        }
    }

    suspend fun addLocation(location: LocationData): Boolean = withContext(Dispatchers.IO) {
        try {
            val entity = PendingLocationEntity.fromLocationData(location)
            dao.insert(entity)
            Log.d(TAG, "✅ Location added to queue")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add location", e)
            false
        }
    }

    suspend fun hasPendingLocations(): Boolean = withContext(Dispatchers.IO) {
        try {
            val count = dao.getCount()
            return@withContext count > 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking pending locations", e)
            return@withContext false
        }
    }

    suspend fun getPendingCount(): Int = withContext(Dispatchers.IO) {
        try {
            return@withContext dao.getCount()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting count", e)
            return@withContext 0
        }
    }

    suspend fun clearAllPendingLocations(): Boolean = withContext(Dispatchers.IO) {
        try {
            val count = dao.getCount()
            if (count == 0) {
                Log.d(TAG, "✅ No locations to clear")
                return@withContext true
            }

            Log.w(TAG, "🗑️ Clearing $count pending locations")
            dao.deleteAll()

            val remaining = dao.getCount()
            if (remaining == 0) {
                Log.d(TAG, "✅ All pending locations cleared successfully")
                return@withContext true
            } else {
                Log.e(TAG, "⚠️ Failed to clear all - $remaining remaining")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing locations", e)
            return@withContext false
        }
    }

    suspend fun backgroundSyncOrClearOldData(
        maxWaitTimeMs: Long = 15_000L,
        maxRetries: Int = MAX_RETRY_ATTEMPTS,
        retryDelayMs: Long = RETRY_DELAY_MS
    ): BackgroundSyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Background sync initiated for old data")

        try {
            val count = dao.getCount()

            if (count == 0) {
                Log.d(TAG, "✅ No old data to handle")
                return@withContext BackgroundSyncResult.NoData
            }

            Log.d(TAG, "📊 Found $count old locations from previous session")

            // ✅ RESTRUCTURED: Try sync with retries
            for (attempt in 1..maxRetries) {
                Log.d(TAG, "🔄 Sync attempt $attempt of $maxRetries...")

                val syncResult = forceSyncAllBeforeCheckout(maxWaitTimeMs)

                // Handle success
                when {
                    syncResult is SyncResult.Success -> {
                        Log.d(TAG, "✅ Background sync SUCCESS on attempt $attempt: ${syncResult.synced} locations saved")
                        return@withContext BackgroundSyncResult.Synced(
                            count = syncResult.synced,
                            attempts = attempt
                        )
                    }

                    syncResult is SyncResult.NetworkError && attempt >= maxRetries -> {
                        Log.w(TAG, "❌ Max retries reached - auto-clearing")
                        clearAllPendingLocations()
                        return@withContext BackgroundSyncResult.ClearedDueToNetwork(count, attempt)
                    }

                    syncResult is SyncResult.Timeout && attempt >= maxRetries -> {
                        Log.w(TAG, "❌ Max retries reached - auto-clearing")
                        clearAllPendingLocations()
                        return@withContext BackgroundSyncResult.ClearedDueToTimeout(
                            syncResult.synced,
                            count - syncResult.synced,
                            attempt
                        )
                    }

                    syncResult is SyncResult.Error && attempt >= maxRetries -> {
                        Log.w(TAG, "❌ Max retries reached - auto-clearing")
                        clearAllPendingLocations()
                        return@withContext BackgroundSyncResult.ClearedDueToError(count, syncResult.message, attempt)
                    }

                    else -> {
                        // Retry on next iteration
                        Log.d(TAG, "⏳ Waiting ${retryDelayMs}ms before retry...")
                        delay(retryDelayMs)
                    }
                }
            }

            // If we get here, all retries failed
            clearAllPendingLocations()
            return@withContext BackgroundSyncResult.ClearedDueToError(count, "Max retries exceeded", maxRetries)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in background sync", e)
            clearAllPendingLocations()
            return@withContext BackgroundSyncResult.ClearedDueToError(0, e.message ?: "Unknown", 1)
        }
    }

    suspend fun forceSyncAllBeforeCheckout(
        maxWaitTimeMs: Long = 30_000L
    ): SyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 Force sync initiated before check-out")

        val startTime = System.currentTimeMillis()
        var totalSynced = 0
        var totalFailed = 0

        try {
            val totalPending = dao.getCount()
            Log.d(TAG, "📊 Total pending locations: $totalPending")

            if (totalPending == 0) {
                Log.d(TAG, "✅ No pending locations to sync")
                return@withContext SyncResult.Success(0)
            }

            // ✅ RESTRUCTURED: Process batches until done or timeout
            while (true) {
                // Check timeout
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime >= maxWaitTimeMs) {
                    Log.w(TAG, "⏱️ Sync timeout reached")
                    return@withContext SyncResult.Timeout(totalSynced, totalFailed)
                }

                // Get next batch
                val batch = dao.getOldestLocations(OPTIMAL_BATCH_SIZE)

                if (batch.isEmpty()) {
                    Log.d(TAG, "✅ All locations synced successfully")
                    return@withContext SyncResult.Success(totalSynced)
                }

                Log.d(TAG, "📤 Syncing batch of ${batch.size} locations...")

                // Sync this batch
                val locations = batch.map { it.toLocationData() }
                val syncOutcome = syncLocations(locations)

                // Handle outcome
                val shouldContinue = handleSyncOutcome(
                    outcome = syncOutcome,
                    batch = batch,
                    onSuccess = { totalSynced += batch.size },
                    onFailure = { totalFailed += batch.size }
                )

                if (!shouldContinue) {
                    Log.w(TAG, "🌐 Network error - stopping sync")
                    return@withContext SyncResult.NetworkError(totalSynced, totalFailed)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Force sync error", e)
            return@withContext SyncResult.Error(totalSynced, totalFailed, e.message ?: "Unknown error")
        }
    }

    private suspend fun handleSyncOutcome(
        outcome: SyncOutcome,
        batch: List<PendingLocationEntity>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ): Boolean {
        return when (outcome) {
            is SyncOutcome.Success -> {
                val ids = batch.map { it.id }
                dao.deleteByIds(ids)
                onSuccess()
                Log.d(TAG, "✅ Batch synced: ${batch.size} locations")
                Log.d(TAG, "📊 Progress: ${dao.getCount()} remaining")
                true // Continue
            }

            is SyncOutcome.Error -> {
                val ids = batch.map { it.id }
                dao.updateSyncAttempts(ids, System.currentTimeMillis())
                onFailure()
                Log.e(TAG, "❌ Batch sync failed: ${outcome.message}")

                // Check if it's a network error
                val isNetworkError = outcome.message.contains("network", ignoreCase = true) ||
                        outcome.message.contains("timeout", ignoreCase = true)
                !isNetworkError // Continue if not network error
            }
        }
    }

    private suspend fun syncLocations(locations: List<LocationData>): SyncOutcome {
        return try {
            val response = apiService.syncLocationTracking(locations)

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Batch upload successful")
                SyncOutcome.Success
            } else {
                Log.e(TAG, "❌ Backend error: ${response.code()}")
                SyncOutcome.Error("Backend error: ${response.code()}")
            }

        } catch (e: IOException) {
            Log.e(TAG, "🌐 Network error during sync", e)
            SyncOutcome.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync error", e)
            SyncOutcome.Error(e.message ?: "Unknown error")
        }
    }

    sealed class BackgroundSyncResult {
        object NoData : BackgroundSyncResult()
        data class Synced(val count: Int, val attempts: Int) : BackgroundSyncResult()
        data class ClearedDueToNetwork(val count: Int, val attempts: Int) : BackgroundSyncResult()
        data class ClearedDueToTimeout(val synced: Int, val cleared: Int, val attempts: Int) : BackgroundSyncResult()
        data class ClearedDueToError(val count: Int, val message: String, val attempts: Int) : BackgroundSyncResult()
        data class Failed(val message: String) : BackgroundSyncResult()
    }

    sealed class SyncResult {
        data class Success(val synced: Int) : SyncResult()
        data class Timeout(val synced: Int, val failed: Int) : SyncResult()
        data class NetworkError(val synced: Int, val failed: Int) : SyncResult()
        data class Error(val synced: Int, val failed: Int, val message: String) : SyncResult()
    }

    private sealed class SyncOutcome {
        object Success : SyncOutcome()
        data class Error(val message: String) : SyncOutcome()
    }
}