package com.hrms.jeejateamozy.feature.location.database

import androidx.room.*

/**
 * DAO for location queue database operations
 * Handles insert, query, delete, and cleanup operations
 */
@Dao
interface PendingLocationDao {

    /**
     * Insert new location to queue
     * Returns the ID of inserted location
     */
    @Insert
    suspend fun insert(location: PendingLocationEntity): Long

    /**
     * Insert multiple locations at once
     */
    @Insert
    suspend fun insertAll(locations: List<PendingLocationEntity>)

    /**
     * Get oldest N pending locations for sync batch
     * Orders by creation time (oldest first)
     */
    @Query("""
        SELECT * FROM pending_locations 
        ORDER BY createdAt ASC 
        LIMIT :limit
    """)
    suspend fun getOldestLocations(limit: Int): List<PendingLocationEntity>

    /**
     * Get all pending locations (for final sync on check-out)
     */
    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC")
    suspend fun getAllLocations(): List<PendingLocationEntity>

    /**
     * Get count of pending locations in queue
     */
    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getCount(): Int

    /**
     * Delete locations by IDs (after successful sync)
     */
    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Delete specific location
     */
    @Delete
    suspend fun delete(location: PendingLocationEntity)

    /**
     * Delete all locations (after 403 error or final cleanup)
     */
    @Query("DELETE FROM pending_locations")
    suspend fun deleteAll()

    /**
     * Update sync attempt metadata
     * Increments attempt counter and updates timestamp
     */
    @Query("""
        UPDATE pending_locations 
        SET syncAttempts = syncAttempts + 1, 
            lastSyncAttempt = :timestamp 
        WHERE id IN (:ids)
    """)
    suspend fun updateSyncAttempts(ids: List<Long>, timestamp: Long)

    /**
     * Delete old failed locations (cleanup)
     * Removes locations older than cutoff time with 10+ failed attempts
     */
    @Query("""
        DELETE FROM pending_locations 
        WHERE createdAt < :cutoffTime 
        AND syncAttempts >= 10
    """)
    suspend fun deleteOldFailedLocations(cutoffTime: Long)

    /**
     * Get locations that need retry
     * Finds locations that haven't been synced in specified time
     */
    @Query("""
        SELECT * FROM pending_locations 
        WHERE lastSyncAttempt IS NULL 
        OR lastSyncAttempt < :cutoffTime
        ORDER BY createdAt ASC
        LIMIT :limit
    """)
    suspend fun getLocationsNeedingRetry(cutoffTime: Long, limit: Int): List<PendingLocationEntity>
}