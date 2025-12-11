package com.hrms.jeejateamozy.feature.location.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for location tracking
 * Provides persistent storage for location queue
 */
@Database(
    entities = [PendingLocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {

    abstract fun pendingLocationDao(): PendingLocationDao

    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null

        /**
         * Get singleton database instance
         */
        fun getInstance(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_tracking.db"
                )
                    .fallbackToDestructiveMigration()  // For development - remove in production
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear database instance (for testing)
         */
        @Synchronized
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}