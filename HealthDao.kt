package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {

    // Daily Activity
    @Query("SELECT * FROM daily_activity ORDER BY date DESC")
    fun observeAllDailyActivity(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getDailyActivityByDate(date: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyActivity(activity: DailyActivityEntity)

    // Workouts
    @Query("SELECT * FROM workouts ORDER BY startTimeMillis DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    // Weight
    @Query("SELECT * FROM weight_entries ORDER BY timestampMillis DESC")
    fun observeAllWeights(): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun deleteWeightById(id: Long)

    // Sleep
    @Query("SELECT * FROM sleep_entries ORDER BY date DESC, id DESC")
    fun observeAllSleep(): Flow<List<SleepEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(entry: SleepEntryEntity)

    @Query("DELETE FROM sleep_entries WHERE id = :id")
    suspend fun deleteSleepById(id: Long)

    // Heart Rate
    @Query("SELECT * FROM heart_rate_entries ORDER BY timestampMillis DESC")
    fun observeAllHeartRate(): Flow<List<HeartRateEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRate(entry: HeartRateEntryEntity)

    @Query("DELETE FROM heart_rate_entries WHERE id = :id")
    suspend fun deleteHeartRateById(id: Long)

    // Water
    @Query("SELECT * FROM water_entries ORDER BY timestampMillis DESC")
    fun observeAllWater(): Flow<List<WaterEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(entry: WaterEntryEntity)

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun deleteWaterById(id: Long)

    // Nutrition
    @Query("SELECT * FROM nutrition_entries ORDER BY timestampMillis DESC")
    fun observeAllNutrition(): Flow<List<NutritionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrition(entry: NutritionEntryEntity)

    @Query("DELETE FROM nutrition_entries WHERE id = :id")
    suspend fun deleteNutritionById(id: Long)

    // Clear all local data (Privacy Control)
    @Query("DELETE FROM daily_activity")
    suspend fun clearAllDailyActivity()

    @Query("DELETE FROM workouts")
    suspend fun clearAllWorkouts()

    @Query("DELETE FROM weight_entries")
    suspend fun clearAllWeights()

    @Query("DELETE FROM sleep_entries")
    suspend fun clearAllSleep()

    @Query("DELETE FROM heart_rate_entries")
    suspend fun clearAllHeartRate()

    @Query("DELETE FROM water_entries")
    suspend fun clearAllWater()

    @Query("DELETE FROM nutrition_entries")
    suspend fun clearAllNutrition()
}
