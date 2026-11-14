package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val steps: Int = 0,
    val activeMinutes: Int = 0,
    val cardioMinutes: Int = 0,
    val caloriesBurnedEst: Int = 0,
    val distanceKm: Float = 0f,
    val isDistanceEstimated: Boolean = true,
    val source: String = "Manual" // "Sensor", "Manual", "Health Connect"
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val categoryId: String,
    val title: String,
    val startTimeMillis: Long,
    val durationSeconds: Long,
    val distanceKm: Double? = null,
    val isDistanceEstimated: Boolean = true,
    val caloriesBurnedEst: Int,
    val stepsCount: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestampMillis: Long,
    val weightKg: Float,
    val source: String = "Manual"
)

@Entity(tableName = "sleep_entries")
data class SleepEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val durationMinutes: Int,
    val bedtimeLabel: String = "",
    val wakeTimeLabel: String = "",
    val qualityLabel: String = "Restful",
    val source: String = "Manual"
)

@Entity(tableName = "heart_rate_entries")
data class HeartRateEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestampMillis: Long,
    val bpm: Int,
    val source: String = "Manual" // "Sensor", "Manual", "Health Connect"
)

@Entity(tableName = "water_entries")
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestampMillis: Long,
    val amountMl: Int,
    val source: String = "Manual"
)

@Entity(tableName = "nutrition_entries")
data class NutritionEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String,
    val timestampMillis: Long,
    val mealName: String,
    val mealType: String,
    val calories: Int,
    val carbsGrams: Float,
    val fatGrams: Float,
    val proteinGrams: Float
)
