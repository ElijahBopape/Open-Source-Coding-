package com.bopape.studentassistant.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The app's local SQLite database (via Room), used for offline mode.
 * A single instance is shared app-wide, as recommended by the Room guide:
 * Android Developers (2026) Save data in a local database using Room.
 * https://developer.android.com/training/data-storage/room
 */
@Database(entities = [TaskEntity::class, CacheEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_assistant.db"
                ).build().also {
                    Log.i("AppDatabase", "Room database opened")
                    instance = it
                }
            }
    }
}
