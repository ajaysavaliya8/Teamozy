package com.hrms.jeejateamozy.feature.location.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PendingLocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocationDatabase : RoomDatabase() {

    abstract fun pendingLocationDao(): PendingLocationDao

    companion object {
        private const val DATABASE_NAME = "teamozy_location_db"

        @Volatile
        private var INSTANCE: LocationDatabase? = null

        fun getInstance(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LocationDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LocationDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}