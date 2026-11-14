package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyActivityEntity::class,
        WorkoutEntity::class,
        WeightEntryEntity::class,
        SleepEntryEntity::class,
        HeartRateEntryEntity::class,
        WaterEntryEntity::class,
        NutritionEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_health_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
