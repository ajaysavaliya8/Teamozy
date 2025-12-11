package com.hrms.jeejateamozy.feature.location.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for pending location operations
 */
@Dao
interface PendingLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: PendingLocationEntity): Long

    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getOldestLocations(limit: Int): List<PendingLocationEntity>

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getCount(): Int

    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM pending_locations")
    suspend fun deleteAll()

    @Query("""
        UPDATE pending_locations 
        SET syncAttempts = syncAttempts + 1, 
            lastSyncAttempt = :timestamp 
        WHERE id IN (:ids)
    """)
    suspend fun updateSyncAttempts(ids: List<Long>, timestamp: Long)

    @Query("SELECT * FROM pending_locations WHERE syncAttempts >= 3 ORDER BY createdAt ASC")
    suspend fun getFailedLocations(): List<PendingLocationEntity>

    @Query("DELETE FROM pending_locations WHERE syncAttempts >= :maxAttempts")
    suspend fun deleteFailedLocations(maxAttempts: Int = 3)
}