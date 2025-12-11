package com.hrms.jeejateamozy.feature.location.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingLocationDao {

    @Insert
    suspend fun insert(location: PendingLocationEntity): Long

    @Query("SELECT * FROM pending_locations ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getOldestLocations(limit: Int): List<PendingLocationEntity>

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getCount(): Int

    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM pending_locations")
    suspend fun deleteAll()

    @Query("UPDATE pending_locations SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :timestamp WHERE id IN (:ids)")
    suspend fun updateSyncAttempts(ids: List<Long>, timestamp: Long)
}