package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailyActivityEntity
import com.example.data.local.HeartRateEntryEntity
import com.example.data.local.NutritionEntryEntity
import com.example.data.local.SleepEntryEntity
import com.example.data.local.WaterEntryEntity
import com.example.data.local.WeightEntryEntity
import com.example.data.local.WorkoutEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserProfileAndSettings
import com.example.data.repository.HealthRepository
import com.example.data.sensors.ActivitySensorManager
import com.example.ui.model.ChartPoint
import com.example.ui.model.DateRangeOption
import com.example.ui.model.HealthConnectStatus
import com.example.ui.model.MetricCardSummary
import com.example.ui.model.MetricType
import com.example.ui.model.TodayDashboardState
import com.example.ui.model.WellnessStrings
import com.example.ui.model.formatDurationMinutes
import com.example.ui.model.todayIsoDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class HealthRawSnapshot(
    val activities: List<DailyActivityEntity> = emptyList(),
    val workouts: List<WorkoutEntity> = emptyList(),
    val weights: List<WeightEntryEntity> = emptyList(),
    val sleeps: List<SleepEntryEntity> = emptyList(),
    val heartRates: List<HeartRateEntryEntity> = emptyList(),
    val waters: List<WaterEntryEntity> = emptyList(),
    val nutritions: List<NutritionEntryEntity> = emptyList(),
    val settings: UserProfileAndSettings = UserProfileAndSettings()
)

data class SensorUiState(
    val hasStepSensor: Boolean = false,
    val hasHeartRateSensor: Boolean = false,
    val hasActivityPermission: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasBodySensorsPermission: Boolean = false,
    val isMeasuringHeartRate: Boolean = false,
    val liveSensorBpm: Int? = null
)

class HealthViewModel(
    private val repository: HealthRepository,
    private val prefs: UserPreferencesRepository,
    private val sensorManager: ActivitySensorManager
) : ViewModel() {

    private var stepListenerJob: Job? = null
    private var hrListenerJob: Job? = null

    private val _healthConnectStatus = MutableStateFlow(HealthConnectStatus())
    val healthConnectStatus: StateFlow<HealthConnectStatus> = _healthConnectStatus

    private val _sensorState = MutableStateFlow(
        SensorUiState(
            hasStepSensor = sensorManager.hasStepCounterSensor,
            hasHeartRateSensor = sensorManager.hasHeartRateSensor,
            hasActivityPermission = sensorManager.hasActivityRecognitionPermission(),
            hasLocationPermission = sensorManager.hasLocationPermission(),
            hasBodySensorsPermission = sensorManager.hasBodySensorsPermission()
        )
    )
    val sensorState: StateFlow<SensorUiState> = _sensorState

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val baseDataFlow = combine(
        repository.dailyActivities,
        repository.workouts,
        repository.weights,
        repository.sleeps,
        repository.heartRates
    ) { activities, workouts, weights, sleeps, heartRates ->
        HealthRawSnapshot(
            activities = activities,
            workouts = workouts,
            weights = weights,
            sleeps = sleeps,
            heartRates = heartRates
        )
    }

    val rawSnapshot: StateFlow<HealthRawSnapshot> = combine(
        baseDataFlow,
        repository.waters,
        repository.nutritions,
        repository.settings
    ) { base, waters, nutritions, settings ->
        base.copy(
            waters = waters,
            nutritions = nutritions,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HealthRawSnapshot()
    )

    val todayState: StateFlow<TodayDashboardState> = rawSnapshot
        .combine(_sensorState) { snap, _ -> buildTodayState(snap) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TodayDashboardState(
                dateLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
            )
        )

    val metricCards: StateFlow<List<MetricCardSummary>> = rawSnapshot
        .combine(_sensorState) { snap, _ -> buildAllMetricCards(snap) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshSensorPermissions()
        refreshHealthConnectStatus()
    }

    fun refreshSensorPermissions() {
        val actGranted = sensorManager.hasActivityRecognitionPermission()
        _sensorState.value = _sensorState.value.copy(
            hasStepSensor = sensorManager.hasStepCounterSensor,
            hasHeartRateSensor = sensorManager.hasHeartRateSensor,
            hasActivityPermission = actGranted,
            hasLocationPermission = sensorManager.hasLocationPermission(),
            hasBodySensorsPermission = sensorManager.hasBodySensorsPermission()
        )
        if (actGranted && sensorManager.hasStepCounterSensor) {
            startStepSensorListenerIfEnabled()
        }
    }

    fun startStepSensorListenerIfEnabled() {
        if (stepListenerJob?.isActive == true) return
        if (!sensorManager.hasActivityRecognitionPermission() || !sensorManager.hasStepCounterSensor) return
        stepListenerJob = viewModelScope.launch {
            val currentSettings = prefs.settingsFlow.first()
            if (!currentSettings.sensorStepTrackingEnabled) return@launch
            sensorManager.observeStepSensorEvents().collect { (value, isCumulative) ->
                val today = todayIsoDate()
                if (!isCumulative) {
                    repository.addSensorSteps(value.toInt().coerceAtLeast(1))
                } else {
                    val saved = prefs.settingsFlow.first()
                    val lastVal = saved.lastSensorCounterValue
                    val lastDate = saved.lastSensorDate
                    if (lastVal < 0f || lastDate != today || value < lastVal) {
                        // First baseline for today or device rebooted
                        prefs.updateLastSensorCounter(value, today)
                    } else {
                        val delta = (value - lastVal).toInt()
                        if (delta > 0) {
                            repository.addSensorSteps(delta)
                            prefs.updateLastSensorCounter(value, today)
                        }
                    }
                }
            }
        }
    }

    fun stopStepSensorListener() {
        stepListenerJob?.cancel()
        stepListenerJob = null
    }

    fun startHeartRateSensorMeasurement() {
        if (!sensorManager.hasHeartRateSensor) {
            _statusMessage.value = "No compatible hardware heart rate sensor detected on this device."
            return
        }
        if (!sensorManager.hasBodySensorsPermission()) {
            _statusMessage.value = "Body sensors permission is needed to read your heart rate sensor."
            return
        }
        hrListenerJob?.cancel()
        _sensorState.value = _sensorState.value.copy(isMeasuringHeartRate = true, liveSensorBpm = null)
        hrListenerJob = viewModelScope.launch {
            sensorManager.observeHeartRateSensor().collect { bpm ->
                _sensorState.value = _sensorState.value.copy(liveSensorBpm = bpm)
            }
        }
    }

    fun stopHeartRateSensorMeasurement(saveLatest: Boolean) {
        val bpm = _sensorState.value.liveSensorBpm
        hrListenerJob?.cancel()
        hrListenerJob = null
        _sensorState.value = _sensorState.value.copy(isMeasuringHeartRate = false)
        if (saveLatest && bpm != null) {
            viewModelScope.launch {
                repository.logHeartRate(bpm = bpm, source = "Sensor")
                _statusMessage.value = "Saved $bpm bpm from heart rate sensor."
            }
        }
    }

    fun refreshHealthConnectStatus(syncMessage: String? = null) {
        viewModelScope.launch {
            _healthConnectStatus.value = repository.healthConnectManager.getStatus(syncMessage)
        }
    }

    fun syncHealthConnectNow() {
        viewModelScope.launch {
            val result = repository.syncFromHealthConnect()
            result.onSuccess { msg ->
                _statusMessage.value = msg
                refreshHealthConnectStatus(msg)
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Unable to sync with Health Connect"
                refreshHealthConnectStatus(err.message)
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Logging actions
    fun logStepsAndActivity(steps: Int, activeMinutes: Int, caloriesEst: Int?, distanceKm: Float?) {
        viewModelScope.launch {
            repository.logManualStepsAndActivity(steps, activeMinutes, caloriesEst, distanceKm)
            _statusMessage.value = "Activity logged"
        }
    }

    fun logWeight(weightKg: Float) {
        viewModelScope.launch {
            repository.logWeight(weightKg)
            _statusMessage.value = "Weight saved"
        }
    }

    fun logSleep(durationMinutes: Int, bedtime: String, wakeTime: String, quality: String) {
        viewModelScope.launch {
            repository.logSleep(durationMinutes, bedtime, wakeTime, quality)
            _statusMessage.value = "Sleep logged"
        }
    }

    fun logHeartRate(bpm: Int) {
        viewModelScope.launch {
            repository.logHeartRate(bpm, source = "Manual")
            _statusMessage.value = "Heart rate logged"
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl)
            _statusMessage.value = "Added $amountMl ml water"
        }
    }

    fun logNutrition(
        mealName: String,
        mealType: String,
        calories: Int,
        carbs: Float,
        fat: Float,
        protein: Float
    ) {
        viewModelScope.launch {
            repository.logNutrition(mealName, mealType, calories, carbs, fat, protein)
            _statusMessage.value = "Meal logged"
        }
    }

    fun deleteWorkout(id: Long) = viewModelScope.launch { repository.deleteWorkout(id) }
    fun deleteWeight(id: Long) = viewModelScope.launch { repository.deleteWeight(id) }
    fun deleteSleep(id: Long) = viewModelScope.launch { repository.deleteSleep(id) }
    fun deleteHeartRate(id: Long) = viewModelScope.launch { repository.deleteHeartRate(id) }
    fun deleteWater(id: Long) = viewModelScope.launch { repository.deleteWater(id) }
    fun deleteNutrition(id: Long) = viewModelScope.launch { repository.deleteNutrition(id) }

    // Settings & Profile actions
    fun updateProfile(name: String, age: Int?, heightCm: Float?, gender: String) {
        viewModelScope.launch {
            prefs.updateProfile(name, age, heightCm, gender)
            _statusMessage.value = "Profile updated"
        }
    }

    fun updateGoals(
        dailySteps: Int,
        dailyWaterMl: Int,
        dailyExerciseMin: Int,
        weeklyCardioMin: Int,
        dailyCalorieGoal: Int
    ) {
        viewModelScope.launch {
            prefs.updateGoals(dailySteps, dailyWaterMl, dailyExerciseMin, weeklyCardioMin, dailyCalorieGoal)
            _statusMessage.value = "Goals updated"
        }
    }

    fun setDarkMode(mode: String) = viewModelScope.launch { prefs.setDarkMode(mode) }
    fun setLanguage(lang: String) = viewModelScope.launch { prefs.setLanguage(lang) }
    fun setWorkoutReminders(enabled: Boolean) = viewModelScope.launch { prefs.setWorkoutReminders(enabled) }
    fun setHydrationReminders(enabled: Boolean) = viewModelScope.launch { prefs.setHydrationReminders(enabled) }
    fun setHealthConnectSync(enabled: Boolean) = viewModelScope.launch { prefs.setHealthConnectSync(enabled) }
    fun setSensorStepTracking(enabled: Boolean) = viewModelScope.launch {
        prefs.setSensorStepTracking(enabled)
        if (enabled) startStepSensorListenerIfEnabled() else stopStepSensorListener()
    }

    fun toggleMetricVisibility(metricId: String) = viewModelScope.launch {
        val current = rawSnapshot.value.settings.visibleMetricIds.toMutableSet()
        if (metricId in current && current.size > 1) {
            current.remove(metricId)
        } else {
            current.add(metricId)
        }
        prefs.setVisibleMetricIds(current)
    }

    fun toggleTodayCardVisibility(cardId: String) = viewModelScope.launch {
        val current = rawSnapshot.value.settings.visibleTodayCardIds.toMutableSet()
        if (cardId in current && current.size > 1) {
            current.remove(cardId)
        } else {
            current.add(cardId)
        }
        prefs.setVisibleTodayCardIds(current)
    }

    fun clearAllLocalData() = viewModelScope.launch {
        repository.clearAllLocalData()
        _statusMessage.value = "All local health records cleared"
    }

    fun getMetricChartPoints(type: MetricType, range: DateRangeOption): List<ChartPoint> {
        return computeChartPoints(rawSnapshot.value, type, range)
    }

    private fun buildTodayState(snap: HealthRawSnapshot): TodayDashboardState {
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weekStartIso = today.minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val todayActivity = snap.activities.firstOrNull { it.date == todayIso }
        val weeklyCardio = snap.activities.filter { it.date >= weekStartIso }.sumOf { it.cardioMinutes }
        val latestSleep = snap.sleeps.firstOrNull { it.date == todayIso } ?: snap.sleeps.firstOrNull()
        val latestHr = snap.heartRates.firstOrNull()
        val todayWater = snap.waters.filter { it.date == todayIso }.sumOf { it.amountMl }
        val todayCaloriesIn = snap.nutritions.filter { it.date == todayIso }.sumOf { it.calories }

        val steps = todayActivity?.steps ?: 0
        val hasStepData = todayActivity != null && steps > 0
        val calsBurned = todayActivity?.caloriesBurnedEst ?: 0
        val hasCalsBurned = todayActivity != null && calsBurned > 0
        val distKm = todayActivity?.distanceKm ?: 0f
        val hasDist = todayActivity != null && distKm > 0f

        // Compute an estimated readiness score ONLY if real sleep, HR, or activity data exists
        val readinessEst = if (latestSleep != null || latestHr != null || hasStepData) {
            var score = 60.0
            var weightCount = 0
            latestSleep?.let {
                val sleepRatio = (it.durationMinutes / 480.0).coerceIn(0.4, 1.15)
                score += (sleepRatio - 0.75) * 45.0
                weightCount++
            }
            if (hasStepData) {
                val stepRatio = (steps.toDouble() / snap.settings.dailyStepGoal).coerceIn(0.0, 1.1)
                score += (stepRatio - 0.5) * 20.0
                weightCount++
            }
            latestHr?.let {
                if (it.bpm in 48..85) score += 10.0 else if (it.bpm > 95) score -= 8.0
                weightCount++
            }
            if (weightCount > 0) score.roundToInt().coerceIn(30, 99) else null
        } else null

        val dateLabel = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

        return TodayDashboardState(
            dateLabel = dateLabel,
            steps = steps,
            hasStepData = hasStepData,
            stepGoal = snap.settings.dailyStepGoal,
            weeklyCardioMinutes = weeklyCardio,
            weeklyCardioGoal = snap.settings.weeklyCardioGoalMin,
            dailyExerciseMinutes = todayActivity?.activeMinutes ?: 0,
            dailyExerciseGoal = snap.settings.dailyExerciseGoalMin,
            caloriesBurnedEst = calsBurned,
            hasCaloriesBurnedData = hasCalsBurned,
            distanceKm = distKm,
            hasDistanceData = hasDist,
            isDistanceEstimated = todayActivity?.isDistanceEstimated ?: true,
            readinessScoreEst = readinessEst,
            latestSleepMinutes = latestSleep?.durationMinutes,
            latestHeartRateBpm = latestHr?.bpm,
            latestHeartRateSource = latestHr?.source,
            waterIntakeMl = todayWater,
            waterGoalMl = snap.settings.dailyWaterGoalMl,
            caloriesIntake = todayCaloriesIn,
            calorieIntakeGoal = snap.settings.dailyCalorieIntakeGoal
        )
    }

    private fun buildAllMetricCards(snap: HealthRawSnapshot): List<MetricCardSummary> {
        val visibleIds = snap.settings.visibleMetricIds
        val lang = snap.settings.language
        val noDataStr = WellnessStrings.noData(lang)
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return MetricType.entries
            .filter { it.id in visibleIds }
            .map { type ->
                val points = computeChartPoints(snap, type, DateRangeOption.WEEKLY)
                val prevPoints = points.dropLast(1).mapNotNull { it.value }
                val prevSummary = if (prevPoints.isEmpty()) {
                    "No recent days"
                } else {
                    val avg = prevPoints.average()
                    when (type) {
                        MetricType.WEIGHT -> String.format(Locale.US, "Prev avg: %.1f kg", avg)
                        MetricType.SLEEP_DURATION -> "Prev avg: ${formatDurationMinutes((avg * 60).roundToInt())}"
                        MetricType.WALKING_DISTANCE -> String.format(Locale.US, "Prev avg: %.2f km", avg)
                        MetricType.EXERCISE_DAYS -> "Active days: ${points.count { (it.value ?: 0f) > 0f }}/7"
                        else -> "Prev avg: ${avg.roundToInt()} ${type.unit}"
                    }
                }

                when (type) {
                    MetricType.WEIGHT -> {
                        val latest = snap.weights.firstOrNull()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = latest?.let { String.format(Locale.US, "%.1f kg", it.weightKg) } ?: noDataStr,
                            hasData = latest != null,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.ENERGY_BURNED -> {
                        val cals = snap.activities.firstOrNull { it.date == todayIso }?.caloriesBurnedEst
                        val has = cals != null && cals > 0
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "Est. $cals kcal" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points,
                            isEstimatedLabel = true
                        )
                    }
                    MetricType.CALORIES_INTAKE -> {
                        val todayMeals = snap.nutritions.filter { it.date == todayIso }
                        val has = todayMeals.isNotEmpty()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "${todayMeals.sumOf { it.calories }} kcal" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.CARBS -> {
                        val todayMeals = snap.nutritions.filter { it.date == todayIso }
                        val has = todayMeals.isNotEmpty()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "${todayMeals.sumOf { it.carbsGrams.toDouble() }.roundToInt()} g" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.FAT -> {
                        val todayMeals = snap.nutritions.filter { it.date == todayIso }
                        val has = todayMeals.isNotEmpty()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "${todayMeals.sumOf { it.fatGrams.toDouble() }.roundToInt()} g" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.PROTEIN -> {
                        val todayMeals = snap.nutritions.filter { it.date == todayIso }
                        val has = todayMeals.isNotEmpty()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "${todayMeals.sumOf { it.proteinGrams.toDouble() }.roundToInt()} g" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.STEPS -> {
                        val steps = snap.activities.firstOrNull { it.date == todayIso }?.steps
                        val has = steps != null && steps > 0
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) String.format(Locale.US, "%,d", steps) else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.EXERCISE_DAYS -> {
                        val activeDaysCount = points.count { (it.value ?: 0f) > 0f }
                        val hasAny = snap.workouts.isNotEmpty() || snap.activities.any { it.activeMinutes > 0 }
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (hasAny) "$activeDaysCount / 7 days" else noDataStr,
                            hasData = hasAny,
                            previousDaysSummary = if (hasAny) "Weekly active days" else "No workouts logged",
                            weeklyPoints = points
                        )
                    }
                    MetricType.HEART_RATE -> {
                        val latest = snap.heartRates.firstOrNull()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = latest?.let { "${it.bpm} bpm" } ?: noDataStr,
                            hasData = latest != null,
                            previousDaysSummary = latest?.let { "Source: ${it.source}" } ?: "No readings yet",
                            weeklyPoints = points
                        )
                    }
                    MetricType.SLEEP_DURATION -> {
                        val latest = snap.sleeps.firstOrNull { it.date == todayIso } ?: snap.sleeps.firstOrNull()
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = latest?.let { formatDurationMinutes(it.durationMinutes) } ?: noDataStr,
                            hasData = latest != null,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                    MetricType.WALKING_DISTANCE -> {
                        val todayAct = snap.activities.firstOrNull { it.date == todayIso }
                        val dist = todayAct?.distanceKm ?: 0f
                        val has = dist > 0f
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) {
                                val prefix = if (todayAct?.isDistanceEstimated != false) "Est. " else ""
                                String.format(Locale.US, "%s%.2f km", prefix, dist)
                            } else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points,
                            isEstimatedLabel = todayAct?.isDistanceEstimated ?: true
                        )
                    }
                    MetricType.WATER_INTAKE -> {
                        val todayWaters = snap.waters.filter { it.date == todayIso }
                        val totalMl = todayWaters.sumOf { it.amountMl }
                        val has = totalMl > 0
                        MetricCardSummary(
                            type = type,
                            currentValueDisplay = if (has) "$totalMl ml" else noDataStr,
                            hasData = has,
                            previousDaysSummary = prevSummary,
                            weeklyPoints = points
                        )
                    }
                }
            }
    }

    private fun computeChartPoints(
        snap: HealthRawSnapshot,
        type: MetricType,
        range: DateRangeOption
    ): List<ChartPoint> {
        val today = LocalDate.now()
        val daysCount = when (range) {
            DateRangeOption.DAILY -> 7 // Daily breakdown of the last 7 days
            DateRangeOption.WEEKLY -> 7
            DateRangeOption.MONTHLY -> 30
        }
        val actMap = snap.activities.associateBy { it.date }
        val workoutsByDate = snap.workouts.groupBy { it.date }
        val weightsByDate = snap.weights.groupBy { it.date }
        val sleepByDate = snap.sleeps.groupBy { it.date }
        val hrByDate = snap.heartRates.groupBy { it.date }
        val waterByDate = snap.waters.groupBy { it.date }
        val nutritionByDate = snap.nutritions.groupBy { it.date }

        return (daysCount - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val label = if (daysCount <= 7) {
                date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            } else {
                date.dayOfMonth.toString()
            }

            val value: Float? = when (type) {
                MetricType.WEIGHT -> weightsByDate[iso]?.firstOrNull()?.weightKg
                MetricType.ENERGY_BURNED -> actMap[iso]?.caloriesBurnedEst?.takeIf { it > 0 }?.toFloat()
                MetricType.CALORIES_INTAKE -> nutritionByDate[iso]?.sumOf { it.calories }?.takeIf { it > 0 }?.toFloat()
                MetricType.CARBS -> nutritionByDate[iso]?.sumOf { it.carbsGrams.toDouble() }?.takeIf { it > 0.0 }?.toFloat()
                MetricType.FAT -> nutritionByDate[iso]?.sumOf { it.fatGrams.toDouble() }?.takeIf { it > 0.0 }?.toFloat()
                MetricType.PROTEIN -> nutritionByDate[iso]?.sumOf { it.proteinGrams.toDouble() }?.takeIf { it > 0.0 }?.toFloat()
                MetricType.STEPS -> actMap[iso]?.steps?.takeIf { it > 0 }?.toFloat()
                MetricType.EXERCISE_DAYS -> {
                    val mins = (actMap[iso]?.activeMinutes ?: 0) +
                        ((workoutsByDate[iso]?.sumOf { it.durationSeconds } ?: 0L) / 60L).toInt()
                    if (mins > 0) mins.toFloat() else null
                }
                MetricType.HEART_RATE -> hrByDate[iso]?.map { it.bpm }?.average()?.toFloat()
                MetricType.SLEEP_DURATION -> sleepByDate[iso]?.sumOf { it.durationMinutes }?.takeIf { it > 0 }?.let { it / 60f }
                MetricType.WALKING_DISTANCE -> actMap[iso]?.distanceKm?.takeIf { it > 0f }
                MetricType.WATER_INTAKE -> waterByDate[iso]?.sumOf { it.amountMl }?.takeIf { it > 0 }?.toFloat()
            }

            ChartPoint(date = date, label = label, value = value)
        }
    }

    class Factory(
        private val repository: HealthRepository,
        private val prefs: UserPreferencesRepository,
        private val sensorManager: ActivitySensorManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HealthViewModel(repository, prefs, sensorManager) as T
        }
    }
}
