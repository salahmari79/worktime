package com.example.mywork.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [WorkSession::class, Task::class], version = 1)
@TypeConverters(Converters::class)
abstract class WorkDatabase : RoomDatabase() {
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: WorkDatabase? = null

        fun getDatabase(context: Context): WorkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkDatabase::class.java,
                    "work_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 