package com.hrms.jeejateamozy.feature.location.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for pending location data
 * Separate database to avoid migrations in main app database
 *
 * VERSION HISTORY:
 * - v1: Initial schema
 * - v2: Schema update (uses destructive migration)
 */
@Database(
    entities = [PendingLocationEntity::class],
    version = 2,  // ← BUMPED from 1 to 2 to fix schema mismatch
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {

    abstract fun pendingLocationDao(): PendingLocationDao

    companion object {
        private const val DATABASE_NAME = "location_tracking.db"

        @Volatile
        private var instance: LocationDatabase? = null

        fun getInstance(context: Context): LocationDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): LocationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LocationDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()  // This will clear old data on schema change
                .build()
        }

        /**
         * Clear instance (for testing)
         */
        fun clearInstance() {
            instance = null
        }
    }
}