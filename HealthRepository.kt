package com.example.data.repository

import com.example.data.healthconnect.HealthConnectManager
import com.example.data.local.DailyActivityEntity
import com.example.data.local.HealthDao
import com.example.data.local.HeartRateEntryEntity
import com.example.data.local.NutritionEntryEntity
import com.example.data.local.SleepEntryEntity
import com.example.data.local.WaterEntryEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WorkoutEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserProfileAndSettings
import com.example.ui.model.WorkoutCategory
import com.example.ui.model.todayIsoDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class HealthRepository(
    private val dao: HealthDao,
    private val prefs: UserPreferencesRepository,
    val healthConnectManager: HealthConnectManager
) {
    val dailyActivities: Flow<List<DailyActivityEntity>> = dao.observeAllDailyActivity()
    val workouts: Flow<List<WorkoutEntity>> = dao.observeAllWorkouts()
    val weights: Flow<List<WeightEntryEntity>> = dao.observeAllWeights()
    val sleeps: Flow<List<SleepEntryEntity>> = dao.observeAllSleep()
    val heartRates: Flow<List<HeartRateEntryEntity>> = dao.observeAllHeartRate()
    val waters: Flow<List<WaterEntryEntity>> = dao.observeAllWater()
    val nutritions: Flow<List<NutritionEntryEntity>> = dao.observeAllNutrition()
    val settings: Flow<UserProfileAndSettings> = prefs.settingsFlow

    suspend fun getLatestWeightKgOrDefault(): Double {
        val list = dao.observeAllWeights().first()
        return list.firstOrNull()?.weightKg?.toDouble() ?: 70.0
    }

    suspend fun addSensorSteps(deltaSteps: Int) {
        if (deltaSteps <= 0) return
        val today = todayIsoDate()
        val current = dao.getDailyActivityByDate(today) ?: DailyActivityEntity(date = today, source = "Sensor")
        val prefsSnapshot = prefs.settingsFlow.first()
        val heightMeters = (prefsSnapshot.heightCm ?: 170f) / 100f
        val strideKmPerStep = (heightMeters * 0.414f) / 1000f
        val newSteps = current.steps + deltaSteps
        val additionalKm = deltaSteps * strideKmPerStep
        val additionalCal = (deltaSteps * 0.04f).roundToInt()

        dao.upsertDailyActivity(
            current.copy(
                steps = newSteps,
                distanceKm = current.distanceKm + additionalKm,
                caloriesBurnedEst = current.caloriesBurnedEst + additionalCal,
                isDistanceEstimated = true,
                source = if (current.source == "Manual") "Sensor + Manual" else "Sensor"
            )
        )
    }

    suspend fun logManualStepsAndActivity(
        steps: Int,
        activeMinutes: Int,
        caloriesEst: Int?,
        distanceKm: Float?,
        date: String = todayIsoDate()
    ) {
        val current = dao.getDailyActivityByDate(date) ?: DailyActivityEntity(date = date)
        val prefsSnapshot = prefs.settingsFlow.first()
        val heightMeters = (prefsSnapshot.heightCm ?: 170f) / 100f
        val strideKmPerStep = (heightMeters * 0.414f) / 1000f
        val addedDist = distanceKm ?: (steps * strideKmPerStep)
        val addedCals = caloriesEst ?: ((steps * 0.04f) + (activeMinutes * 5)).roundToInt()

        dao.upsertDailyActivity(
            current.copy(
                steps = current.steps + steps,
                activeMinutes = current.activeMinutes + activeMinutes,
                cardioMinutes = current.cardioMinutes + activeMinutes,
                caloriesBurnedEst = current.caloriesBurnedEst + addedCals,
                distanceKm = current.distanceKm + addedDist,
                isDistanceEstimated = distanceKm == null,
                source = "Manual"
            )
        )
    }

    suspend fun saveWorkout(
        category: WorkoutCategory,
        startTimeMillis: Long,
        durationSeconds: Long,
        distanceKm: Double?,
        isDistanceEstimated: Boolean,
        caloriesBurnedEst: Int,
        stepsCount: Int,
        notes: String = ""
    ) {
        val date = todayIsoDate()
        dao.insertWorkout(
            WorkoutEntity(
                date = date,
                categoryId = category.id,
                title = category.displayName,
                startTimeMillis = startTimeMillis,
                durationSeconds = durationSeconds,
                distanceKm = distanceKm,
                isDistanceEstimated = isDistanceEstimated,
                caloriesBurnedEst = caloriesBurnedEst,
                stepsCount = stepsCount,
                notes = notes
            )
        )

        val mins = (durationSeconds / 60L).toInt().coerceAtLeast(1)
        val current = dao.getDailyActivityByDate(date) ?: DailyActivityEntity(date = date)
        val cardioAdd = if (category in listOf(
                WorkoutCategory.WALKING,
                WorkoutCategory.RUNNING,
                WorkoutCategory.CYCLING,
                WorkoutCategory.FOOTBALL,
                WorkoutCategory.OTHER
            )
        ) mins else (mins / 2)

        dao.upsertDailyActivity(
            current.copy(
                steps = current.steps + stepsCount,
                activeMinutes = current.activeMinutes + mins,
                cardioMinutes = current.cardioMinutes + cardioAdd,
                caloriesBurnedEst = current.caloriesBurnedEst + caloriesBurnedEst,
                distanceKm = current.distanceKm + (distanceKm?.toFloat() ?: 0f),
                isDistanceEstimated = if (distanceKm != null) isDistanceEstimated else current.isDistanceEstimated
            )
        )

        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeWorkout(
                category = category,
                startTimeMillis = startTimeMillis,
                durationSeconds = durationSeconds,
                distanceKm = distanceKm,
                caloriesEst = caloriesBurnedEst
            )
        }
    }

    suspend fun deleteWorkout(id: Long) = dao.deleteWorkoutById(id)

    suspend fun logWeight(weightKg: Float, date: String = todayIsoDate()) {
        val now = System.currentTimeMillis()
        dao.insertWeight(
            WeightEntryEntity(
                date = date,
                timestampMillis = now,
                weightKg = weightKg,
                source = "Manual"
            )
        )
        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeWeight(weightKg, now)
        }
    }

    suspend fun deleteWeight(id: Long) = dao.deleteWeightById(id)

    suspend fun logSleep(
        durationMinutes: Int,
        bedtimeLabel: String = "",
        wakeTimeLabel: String = "",
        qualityLabel: String = "Restful",
        date: String = todayIsoDate()
    ) {
        dao.insertSleep(
            SleepEntryEntity(
                date = date,
                durationMinutes = durationMinutes,
                bedtimeLabel = bedtimeLabel,
                wakeTimeLabel = wakeTimeLabel,
                qualityLabel = qualityLabel,
                source = "Manual"
            )
        )
        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeSleep(durationMinutes)
        }
    }

    suspend fun deleteSleep(id: Long) = dao.deleteSleepById(id)

    suspend fun logHeartRate(bpm: Int, source: String = "Manual", date: String = todayIsoDate()) {
        val now = System.currentTimeMillis()
        dao.insertHeartRate(
            HeartRateEntryEntity(
                date = date,
                timestampMillis = now,
                bpm = bpm,
                source = source
            )
        )
        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeHeartRate(bpm, now)
        }
    }

    suspend fun deleteHeartRate(id: Long) = dao.deleteHeartRateById(id)

    suspend fun logWater(amountMl: Int, date: String = todayIsoDate()) {
        val now = System.currentTimeMillis()
        dao.insertWater(
            WaterEntryEntity(
                date = date,
                timestampMillis = now,
                amountMl = amountMl,
                source = "Manual"
            )
        )
        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeWater(amountMl, now)
        }
    }

    suspend fun deleteWater(id: Long) = dao.deleteWaterById(id)

    suspend fun logNutrition(
        mealName: String,
        mealType: String,
        calories: Int,
        carbsGrams: Float,
        fatGrams: Float,
        proteinGrams: Float,
        date: String = todayIsoDate()
    ) {
        val now = System.currentTimeMillis()
        dao.insertNutrition(
            NutritionEntryEntity(
                date = date,
                timestampMillis = now,
                mealName = mealName,
                mealType = mealType,
                calories = calories,
                carbsGrams = carbsGrams,
                fatGrams = fatGrams,
                proteinGrams = proteinGrams
            )
        )
        if (prefs.settingsFlow.first().healthConnectSyncEnabled) {
            healthConnectManager.writeNutrition(
                mealName = mealName,
                calories = calories,
                carbsGrams = carbsGrams,
                fatGrams = fatGrams,
                proteinGrams = proteinGrams,
                timestampMillis = now
            )
        }
    }

    suspend fun deleteNutrition(id: Long) = dao.deleteNutritionById(id)

    suspend fun syncFromHealthConnect(): Result<String> {
        val result = healthConnectManager.syncRecentData()
        return result.map { payload ->
            var importedCount = 0
            val allDates = (payload.dailyStepsByDate.keys +
                payload.dailyDistanceKmByDate.keys +
                payload.dailyCaloriesByDate.keys).distinct()

            for (date in allDates) {
                val existing = dao.getDailyActivityByDate(date) ?: DailyActivityEntity(date = date)
                val hcSteps = payload.dailyStepsByDate[date] ?: 0
                val hcDist = payload.dailyDistanceKmByDate[date] ?: 0f
                val hcCal = payload.dailyCaloriesByDate[date] ?: 0
                dao.upsertDailyActivity(
                    existing.copy(
                        steps = maxOf(existing.steps, hcSteps),
                        distanceKm = maxOf(existing.distanceKm, hcDist),
                        caloriesBurnedEst = maxOf(existing.caloriesBurnedEst, hcCal),
                        source = "Health Connect"
                    )
                )
                importedCount++
            }

            val existingWeights = dao.observeAllWeights().first().map { it.timestampMillis }.toSet()
            payload.weights.filter { it.timestampMillis !in existingWeights }.forEach {
                dao.insertWeight(it)
                importedCount++
            }

            val existingHr = dao.observeAllHeartRate().first().map { it.timestampMillis }.toSet()
            payload.heartRates.filter { it.timestampMillis !in existingHr }.forEach {
                dao.insertHeartRate(it)
                importedCount++
            }

            val existingSleepDates = dao.observeAllSleep().first().map { "${it.date}_${it.durationMinutes}" }.toSet()
            payload.sleeps.filter { "${it.date}_${it.durationMinutes}" !in existingSleepDates }.forEach {
                dao.insertSleep(it)
                importedCount++
            }

            val existingWater = dao.observeAllWater().first().map { it.timestampMillis }.toSet()
            payload.waters.filter { it.timestampMillis !in existingWater }.forEach {
                dao.insertWater(it)
                importedCount++
            }

            "Synced $importedCount Health Connect records"
        }
    }

    suspend fun clearAllLocalData() {
        dao.clearAllDailyActivity()
        dao.clearAllWorkouts()
        dao.clearAllWeights()
        dao.clearAllSleep()
        dao.clearAllHeartRate()
        dao.clearAllWater()
        dao.clearAllNutrition()
    }
}
