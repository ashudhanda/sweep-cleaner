package com.sweep.cleaner.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrashEntity::class], version = 1, exportSchema = false)
abstract class SweepDatabase : RoomDatabase() {

    abstract fun trashDao(): TrashDao

    companion object {
        @Volatile
        private var INSTANCE: SweepDatabase? = null

        fun getInstance(context: Context): SweepDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SweepDatabase::class.java,
                    "sweep_cleaner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
