package com.example.data.healthconnect

import android.content.Context
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.example.data.local.DailyActivityEntity
import com.example.data.local.HeartRateEntryEntity
import com.example.data.local.SleepEntryEntity
import com.example.data.local.WaterEntryEntity
import com.example.data.local.WeightEntryEntity
import com.example.ui.model.HealthConnectStatus
import com.example.ui.model.WorkoutCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class HealthConnectSyncPayload(
    val dailyStepsByDate: Map<String, Int> = emptyMap(),
    val dailyDistanceKmByDate: Map<String, Float> = emptyMap(),
    val dailyCaloriesByDate: Map<String, Int> = emptyMap(),
    val weights: List<WeightEntryEntity> = emptyList(),
    val heartRates: List<HeartRateEntryEntity> = emptyList(),
    val sleeps: List<SleepEntryEntity> = emptyList(),
    val waters: List<WaterEntryEntity> = emptyList()
)

class HealthConnectManager(private val context: Context) {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class)
    )

    private fun getClientOrNull(): HealthConnectClient? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else null
        }.getOrNull()
    }

    suspend fun getStatus(lastSyncMessage: String? = null): HealthConnectStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return HealthConnectStatus(
                isAvailable = false,
                needsProviderUpdate = false,
                isSupportedDevice = false,
                hasPermissions = false,
                lastSyncMessage = "Health Connect requires Android 9 or newer"
            )
        }
        return runCatching {
            val sdkStatus = HealthConnectClient.getSdkStatus(context)
            when (sdkStatus) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val client = HealthConnectClient.getOrCreate(context)
                    val granted = client.permissionController.getGrantedPermissions()
                    val hasAny = granted.containsAll(requiredPermissions) ||
                        granted.any { it in requiredPermissions }
                    HealthConnectStatus(
                        isAvailable = true,
                        needsProviderUpdate = false,
                        isSupportedDevice = true,
                        hasPermissions = hasAny,
                        lastSyncMessage = lastSyncMessage
                    )
                }
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    HealthConnectStatus(
                        isAvailable = false,
                        needsProviderUpdate = true,
                        isSupportedDevice = true,
                        hasPermissions = false,
                        lastSyncMessage = "Health Connect provider update required"
                    )
                }
                else -> {
                    HealthConnectStatus(
                        isAvailable = false,
                        needsProviderUpdate = false,
                        isSupportedDevice = true,
                        hasPermissions = false,
                        lastSyncMessage = "Health Connect not installed on device"
                    )
                }
            }
        }.getOrElse {
            HealthConnectStatus(
                isAvailable = false,
                isSupportedDevice = true,
                hasPermissions = false,
                lastSyncMessage = lastSyncMessage
            )
        }
    }

    suspend fun syncRecentData(): Result<HealthConnectSyncPayload> = runCatching {
        val client = getClientOrNull()
            ?: throw IllegalStateException("Health Connect is unavailable on this device.")
        val granted = client.permissionController.getGrantedPermissions()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)

        val stepsMap = mutableMapOf<String, Int>()
        val distMap = mutableMapOf<String, Float>()
        val calMap = mutableMapOf<String, Int>()

        // Aggregate last 7 days of steps, distance, and calories
        for (offset in 0..6) {
            val date = today.minusDays(offset.toLong())
            val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            val metrics = buildSet {
                if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
                    add(StepsRecord.COUNT_TOTAL)
                }
                if (HealthPermission.getReadPermission(DistanceRecord::class) in granted) {
                    add(DistanceRecord.DISTANCE_TOTAL)
                }
                if (HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class) in granted) {
                    add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
                }
            }
            if (metrics.isNotEmpty()) {
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = metrics,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                )
                agg[StepsRecord.COUNT_TOTAL]?.let { if (it > 0) stepsMap[iso] = it.toInt() }
                agg[DistanceRecord.DISTANCE_TOTAL]?.let {
                    if (it.inKilometers > 0) distMap[iso] = it.inKilometers.toFloat()
                }
                agg[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.let {
                    if (it.inKilocalories > 0) calMap[iso] = it.inKilocalories.toInt()
                }
            }
        }

        val rangeStart = today.minusDays(14).atStartOfDay(zone).toInstant()
        val rangeEnd = Instant.now()

        val weights = if (HealthPermission.getReadPermission(WeightRecord::class) in granted) {
            client.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(rangeStart, rangeEnd))
            ).records.map {
                val localDate = it.time.atZone(zone).toLocalDate()
                WeightEntryEntity(
                    date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    timestampMillis = it.time.toEpochMilli(),
                    weightKg = it.weight.inKilograms.toFloat(),
                    source = "Health Connect"
                )
            }
        } else emptyList()

        val heartRates = if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
            client.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(rangeStart, rangeEnd))
            ).records.flatMap { rec ->
                rec.samples.takeLast(5).map { sample ->
                    val localDate = sample.time.atZone(zone).toLocalDate()
                    HeartRateEntryEntity(
                        date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        timestampMillis = sample.time.toEpochMilli(),
                        bpm = sample.beatsPerMinute.toInt(),
                        source = "Health Connect"
                    )
                }
            }
        } else emptyList()

        val sleeps = if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) {
            client.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(rangeStart, rangeEnd))
            ).records.mapNotNull { rec ->
                val mins = ChronoUnit.MINUTES.between(rec.startTime, rec.endTime).toInt()
                if (mins <= 0) null else {
                    val localDate = rec.endTime.atZone(zone).toLocalDate()
                    SleepEntryEntity(
                        date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        durationMinutes = mins,
                        qualityLabel = rec.title ?: "Synced Sleep",
                        source = "Health Connect"
                    )
                }
            }
        } else emptyList()

        val waters = if (HealthPermission.getReadPermission(HydrationRecord::class) in granted) {
            client.readRecords(
                ReadRecordsRequest(HydrationRecord::class, TimeRangeFilter.between(rangeStart, rangeEnd))
            ).records.map { rec ->
                val localDate = rec.endTime.atZone(zone).toLocalDate()
                WaterEntryEntity(
                    date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    timestampMillis = rec.endTime.toEpochMilli(),
                    amountMl = (rec.volume.inLiters * 1000.0).toInt(),
                    source = "Health Connect"
                )
            }
        } else emptyList()

        HealthConnectSyncPayload(
            dailyStepsByDate = stepsMap,
            dailyDistanceKmByDate = distMap,
            dailyCaloriesByDate = calMap,
            weights = weights,
            heartRates = heartRates,
            sleeps = sleeps,
            waters = waters
        )
    }

    suspend fun writeWeight(weightKg: Float, timestampMillis: Long) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(WeightRecord::class) !in granted) return@runCatching
        val instant = Instant.ofEpochMilli(timestampMillis)
        val offset = ZoneOffset.systemDefault().rules.getOffset(instant)
        client.insertRecords(
            listOf(
                WeightRecord(
                    weight = Mass.kilograms(weightKg.toDouble()),
                    time = instant,
                    zoneOffset = offset,
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }

    suspend fun writeWater(amountMl: Int, timestampMillis: Long) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(HydrationRecord::class) !in granted) return@runCatching
        val end = Instant.ofEpochMilli(timestampMillis)
        val start = end.minusSeconds(60)
        val offset = ZoneOffset.systemDefault().rules.getOffset(end)
        client.insertRecords(
            listOf(
                HydrationRecord(
                    volume = Volume.liters(amountMl / 1000.0),
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }

    suspend fun writeHeartRate(bpm: Int, timestampMillis: Long) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(HeartRateRecord::class) !in granted) return@runCatching
        val instant = Instant.ofEpochMilli(timestampMillis)
        val offset = ZoneOffset.systemDefault().rules.getOffset(instant)
        client.insertRecords(
            listOf(
                HeartRateRecord(
                    startTime = instant,
                    endTime = instant.plusSeconds(1),
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    samples = listOf(HeartRateRecord.Sample(time = instant, beatsPerMinute = bpm.toLong())),
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }

    suspend fun writeSleep(durationMinutes: Int) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(SleepSessionRecord::class) !in granted) return@runCatching
        val end = Instant.now()
        val start = end.minus(durationMinutes.toLong(), ChronoUnit.MINUTES)
        val offset = ZoneOffset.systemDefault().rules.getOffset(end)
        client.insertRecords(
            listOf(
                SleepSessionRecord(
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    title = "Logged Sleep",
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }

    suspend fun writeWorkout(
        category: WorkoutCategory,
        startTimeMillis: Long,
        durationSeconds: Long,
        distanceKm: Double?,
        caloriesEst: Int
    ) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        val start = Instant.ofEpochMilli(startTimeMillis)
        val end = start.plusSeconds(durationSeconds.coerceAtLeast(1))
        val offset = ZoneOffset.systemDefault().rules.getOffset(end)
        val records = mutableListOf<androidx.health.connect.client.records.Record>()

        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) in granted) {
            val hcType = when (category) {
                WorkoutCategory.WALKING -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
                WorkoutCategory.RUNNING -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                WorkoutCategory.CYCLING -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
                WorkoutCategory.GYM -> ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS
                WorkoutCategory.STRENGTH_TRAINING -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
                WorkoutCategory.FOOTBALL -> ExerciseSessionRecord.EXERCISE_TYPE_FOOTBALL_AMERICAN
                WorkoutCategory.OTHER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }
            records.add(
                ExerciseSessionRecord(
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    exerciseType = hcType,
                    title = category.displayName,
                    metadata = Metadata.manualEntry()
                )
            )
        }
        if (distanceKm != null && distanceKm > 0 &&
            HealthPermission.getWritePermission(DistanceRecord::class) in granted
        ) {
            records.add(
                DistanceRecord(
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    distance = Length.kilometers(distanceKm),
                    metadata = Metadata.manualEntry()
                )
            )
        }
        if (caloriesEst > 0 &&
            HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class) in granted
        ) {
            records.add(
                TotalCaloriesBurnedRecord(
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    energy = Energy.kilocalories(caloriesEst.toDouble()),
                    metadata = Metadata.manualEntry()
                )
            )
        }
        if (records.isNotEmpty()) {
            client.insertRecords(records)
        }
    }

    suspend fun writeNutrition(
        mealName: String,
        calories: Int,
        carbsGrams: Float,
        fatGrams: Float,
        proteinGrams: Float,
        timestampMillis: Long
    ) = runCatching {
        val client = getClientOrNull() ?: return@runCatching
        val granted = client.permissionController.getGrantedPermissions()
        if (HealthPermission.getWritePermission(NutritionRecord::class) !in granted) return@runCatching
        val end = Instant.ofEpochMilli(timestampMillis)
        val start = end.minusSeconds(60)
        val offset = ZoneOffset.systemDefault().rules.getOffset(end)
        client.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = start,
                    endTime = end,
                    startZoneOffset = offset,
                    endZoneOffset = offset,
                    name = mealName,
                    energy = Energy.kilocalories(calories.toDouble()),
                    totalCarbohydrate = Mass.grams(carbsGrams.toDouble()),
                    totalFat = Mass.grams(fatGrams.toDouble()),
                    protein = Mass.grams(proteinGrams.toDouble()),
                    metadata = Metadata.manualEntry()
                )
            )
        )
    }
}
