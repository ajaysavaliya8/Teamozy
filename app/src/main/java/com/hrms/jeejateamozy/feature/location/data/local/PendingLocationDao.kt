package com.hrms.jeejateamozy.feature.location.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for pending location operations
 */
@Dao
interface PendingLocationDao {

    /**
     * Insert a new location into the queue
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: PendingLocationEntity): Long

    /**
     * Insert multiple locations at once
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<PendingLocationEntity>): List<Long>

    /**
     * Get oldest N locations for sync (FIFO order)
     */
    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getOldestLocations(limit: Int): List<PendingLocationEntity>

    /**
     * Get total count of pending locations
     */
    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getCount(): Int

    /**
     * Delete locations by their IDs (after successful sync)
     */
    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Delete all pending locations (on 403 or logout)
     */
    @Query("DELETE FROM pending_locations")
    suspend fun deleteAll()

    /**
     * Get all pending locations (for debugging)
     */
    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingLocationEntity>
}