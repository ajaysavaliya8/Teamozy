package com.hrms.jeejateamozy.feature.location.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for pending location data
 */
@Database(
    entities = [PendingLocationEntity::class],
    version = 1,
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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}