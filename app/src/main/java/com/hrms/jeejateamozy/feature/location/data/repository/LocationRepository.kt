package com.hrms.jeejateamozy.feature.location.data.repository

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.ApiService
import com.hrms.jeejateamozy.feature.location.data.local.LocationDatabase
import com.hrms.jeejateamozy.feature.location.data.local.PendingLocationDao
import com.hrms.jeejateamozy.feature.location.data.local.PendingLocationEntity
import com.hrms.jeejateamozy.feature.location.data.remote.LocationSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository for location tracking operations
 * Handles local storage and server sync
 */
class LocationRepository(
    private val context: Context,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "LocationRepository"
        private const val MAX_BATCH_SIZE = 50  // API allows 100, use 50 for reliability
    }

    private val dao: PendingLocationDao by lazy {
        LocationDatabase.getInstance(context).pendingLocationDao()
    }

    // ==========================================
    // LOCAL STORAGE OPERATIONS
    // ==========================================

    /**
     * Store a location in the local queue
     */
    suspend fun storeLocation(location: PendingLocationEntity): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val id = dao.insert(location)
                Log.d(TAG, "📍 Location stored locally (id=$id)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to store location", e)
                false
            }
        }
    }

    /**
     * Get count of pending locations
     */
    suspend fun getPendingCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                dao.getCount()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to get pending count", e)
                0
            }
        }
    }

    /**
     * Clear all pending locations
     */
    suspend fun clearAllLocations(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dao.deleteAll()
                Log.d(TAG, "🗑️ All pending locations cleared")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to clear locations", e)
                false
            }
        }
    }

    // ==========================================
    // SYNC OPERATIONS
    // ==========================================

    /**
     * Sync pending locations to server
     * Returns SyncResult indicating outcome
     */
    suspend fun syncPendingLocations(): SyncResult {
        return withContext(Dispatchers.IO) {
            try {
                val pendingCount = dao.getCount()

                if (pendingCount == 0) {
                    Log.d(TAG, "✅ No pending locations to sync")
                    return@withContext SyncResult.Success(synced = 0, remaining = 0)
                }

                Log.d(TAG, "🔄 Starting sync - $pendingCount locations pending")

                // Get batch of oldest locations
                val batch = dao.getOldestLocations(MAX_BATCH_SIZE)

                if (batch.isEmpty()) {
                    return@withContext SyncResult.Success(synced = 0, remaining = 0)
                }

                // Convert to API format
                val requestBody = batch.map { LocationSyncRequest.fromEntity(it) }

                Log.d(TAG, "📤 Syncing batch of ${batch.size} locations...")

                // Call API
                val response = apiService.syncLocationTracking(requestBody)

                when {
                    response.isSuccessful -> {
                        // Success - delete synced locations
                        val ids = batch.map { it.id }
                        dao.deleteByIds(ids)

                        val remaining = dao.getCount()
                        Log.d(TAG, "✅ Sync successful - ${batch.size} synced, $remaining remaining")

                        SyncResult.Success(synced = batch.size, remaining = remaining)
                    }

                    response.code() == 403 -> {
                        // No active session - stop tracking
                        Log.w(TAG, "⚠️ 403 received - no active session")
                        SyncResult.SessionEnded
                    }

                    else -> {
                        Log.e(TAG, "❌ Sync failed - HTTP ${response.code()}")
                        SyncResult.Error(
                            code = response.code(),
                            message = "Server error: ${response.code()}"
                        )
                    }
                }

            } catch (e: IOException) {
                Log.e(TAG, "🌐 Network error during sync", e)
                SyncResult.NetworkError(e.message ?: "Network error")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync error", e)
                SyncResult.Error(code = -1, message = e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Force sync all locations (before check-out)
     * Attempts to sync all pending locations with timeout
     */
    suspend fun forceSyncAll(maxAttempts: Int = 3): ForceSyncResult {
        return withContext(Dispatchers.IO) {
            var totalSynced = 0
            var attempts = 0

            while (attempts < maxAttempts) {
                val pendingCount = dao.getCount()

                if (pendingCount == 0) {
                    Log.d(TAG, "✅ Force sync complete - $totalSynced total synced")
                    return@withContext ForceSyncResult.Success(totalSynced)
                }

                Log.d(TAG, "🔄 Force sync attempt ${attempts + 1}/$maxAttempts - $pendingCount pending")

                when (val result = syncPendingLocations()) {
                    is SyncResult.Success -> {
                        totalSynced += result.synced

                        if (result.remaining == 0) {
                            Log.d(TAG, "✅ Force sync complete - $totalSynced total synced")
                            return@withContext ForceSyncResult.Success(totalSynced)
                        }

                        // Continue syncing remaining
                        attempts++
                    }

                    is SyncResult.SessionEnded -> {
                        Log.w(TAG, "⚠️ Session ended during force sync")
                        return@withContext ForceSyncResult.SessionEnded(totalSynced)
                    }

                    is SyncResult.NetworkError -> {
                        Log.e(TAG, "🌐 Network error during force sync")
                        return@withContext ForceSyncResult.NetworkError(totalSynced, result.message)
                    }

                    is SyncResult.Error -> {
                        Log.e(TAG, "❌ Error during force sync: ${result.message}")
                        attempts++
                    }
                }
            }

            // Max attempts reached
            val remaining = dao.getCount()
            Log.w(TAG, "⚠️ Force sync incomplete - $totalSynced synced, $remaining remaining")
            ForceSyncResult.Partial(totalSynced, remaining)
        }
    }
}

// ==========================================
// RESULT CLASSES
// ==========================================

/**
 * Result of a single sync operation
 */
sealed class SyncResult {
    data class Success(val synced: Int, val remaining: Int) : SyncResult()
    data object SessionEnded : SyncResult()
    data class NetworkError(val message: String) : SyncResult()
    data class Error(val code: Int, val message: String) : SyncResult()
}

/**
 * Result of force sync (sync all) operation
 */
sealed class ForceSyncResult {
    data class Success(val totalSynced: Int) : ForceSyncResult()
    data class Partial(val synced: Int, val remaining: Int) : ForceSyncResult()
    data class SessionEnded(val syncedBeforeEnd: Int) : ForceSyncResult()
    data class NetworkError(val synced: Int, val message: String) : ForceSyncResult()
}