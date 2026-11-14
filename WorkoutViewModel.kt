package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.WorkoutEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.HealthRepository
import com.example.data.sensors.ActivitySensorManager
import com.example.ui.model.ActiveWorkoutState
import com.example.ui.model.WorkoutCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class WorkoutViewModel(
    private val repository: HealthRepository,
    private val prefs: UserPreferencesRepository,
    private val sensorManager: ActivitySensorManager
) : ViewModel() {

    private val _activeWorkout = MutableStateFlow(ActiveWorkoutState())
    val activeWorkout: StateFlow<ActiveWorkoutState> = _activeWorkout

    private val _savedMessage = MutableStateFlow<String?>(null)
    val savedMessage: StateFlow<String?> = _savedMessage

    val workoutHistory: StateFlow<List<WorkoutEntity>> = repository.workouts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null
    private var gpsJob: Job? = null
    private var stepJob: Job? = null
    private var userWeightKg: Double = 70.0
    private var strideMeters: Double = 0.74
    private var lastCumulativeCounter: Float = -1f

    fun selectCategory(category: WorkoutCategory) {
        if (_activeWorkout.value.isRunning) return
        _activeWorkout.value = _activeWorkout.value.copy(category = category)
    }

    fun startWorkout() {
        if (_activeWorkout.value.isRunning) return
        val category = _activeWorkout.value.category
        lastCumulativeCounter = -1f

        viewModelScope.launch {
            userWeightKg = repository.getLatestWeightKgOrDefault()
            val heightCm = prefs.settingsFlow.first().heightCm ?: 170f
            strideMeters = (heightCm / 100.0) * 0.414
        }

        _activeWorkout.value = ActiveWorkoutState(
            isRunning = true,
            isPaused = false,
            category = category,
            elapsedSeconds = 0L,
            distanceKm = 0.0,
            isDistanceGpsBacked = false,
            stepsTaken = 0,
            estimatedCalories = 0,
            startTimeMillis = System.currentTimeMillis()
        )

        startTickerAndTrackers()
    }

    fun togglePauseResume() {
        val current = _activeWorkout.value
        if (!current.isRunning) return
        if (current.isPaused) {
            _activeWorkout.value = current.copy(isPaused = false)
            startTickerAndTrackers()
        } else {
            _activeWorkout.value = current.copy(isPaused = true)
            stopTrackersOnly()
        }
    }

    fun stopAndSaveWorkout(notes: String = "") {
        val current = _activeWorkout.value
        if (!current.isRunning) return
        stopTrackersOnly()

        val duration = current.elapsedSeconds.coerceAtLeast(1L)
        val distToSave = if (current.category.tracksDistance && current.distanceKm > 0.005) {
            current.distanceKm
        } else null

        viewModelScope.launch {
            repository.saveWorkout(
                category = current.category,
                startTimeMillis = current.startTimeMillis,
                durationSeconds = duration,
                distanceKm = distToSave,
                isDistanceEstimated = !current.isDistanceGpsBacked,
                caloriesBurnedEst = current.estimatedCalories.coerceAtLeast(1),
                stepsCount = current.stepsTaken,
                notes = notes
            )
            _activeWorkout.value = ActiveWorkoutState(category = current.category)
            _savedMessage.value = "Saved ${current.category.displayName} workout"
        }
    }

    fun discardWorkout() {
        stopTrackersOnly()
        _activeWorkout.value = ActiveWorkoutState(category = _activeWorkout.value.category)
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    fun clearSavedMessage() {
        _savedMessage.value = null
    }

    fun hasLocationPermission(): Boolean = sensorManager.hasLocationPermission()
    fun hasActivityPermission(): Boolean = sensorManager.hasActivityRecognitionPermission()

    private fun startTickerAndTrackers() {
        stopTrackersOnly()

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val curr = _activeWorkout.value
                if (!curr.isRunning || curr.isPaused) break
                val newSeconds = curr.elapsedSeconds + 1L
                val hours = newSeconds / 3600.0
                val estCals = (curr.category.metValue * userWeightKg * hours).roundToInt()
                _activeWorkout.value = curr.copy(
                    elapsedSeconds = newSeconds,
                    estimatedCalories = estCals
                )
            }
        }

        if (_activeWorkout.value.category.tracksDistance && sensorManager.hasLocationPermission()) {
            gpsJob = viewModelScope.launch {
                sensorManager.observeWorkoutDistanceMeters().collect { deltaMeters ->
                    val curr = _activeWorkout.value
                    if (curr.isRunning && !curr.isPaused) {
                        _activeWorkout.value = curr.copy(
                            distanceKm = curr.distanceKm + (deltaMeters / 1000.0),
                            isDistanceGpsBacked = true
                        )
                    }
                }
            }
        }

        if (sensorManager.hasActivityRecognitionPermission() && sensorManager.hasStepCounterSensor) {
            stepJob = viewModelScope.launch {
                sensorManager.observeStepSensorEvents().collect { (value, isCumulative) ->
                    val curr = _activeWorkout.value
                    if (!curr.isRunning || curr.isPaused) return@collect
                    val deltaSteps = if (!isCumulative) {
                        value.toInt().coerceAtLeast(1)
                    } else {
                        if (lastCumulativeCounter < 0f || value < lastCumulativeCounter) {
                            lastCumulativeCounter = value
                            0
                        } else {
                            val d = (value - lastCumulativeCounter).toInt()
                            lastCumulativeCounter = value
                            d
                        }
                    }
                    if (deltaSteps > 0) {
                        val addedDistKm = if (!curr.isDistanceGpsBacked && curr.category.tracksDistance) {
                            (deltaSteps * strideMeters) / 1000.0
                        } else 0.0
                        _activeWorkout.value = curr.copy(
                            stepsTaken = curr.stepsTaken + deltaSteps,
                            distanceKm = curr.distanceKm + addedDistKm
                        )
                    }
                }
            }
        }
    }

    private fun stopTrackersOnly() {
        timerJob?.cancel()
        timerJob = null
        gpsJob?.cancel()
        gpsJob = null
        stepJob?.cancel()
        stepJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTrackersOnly()
    }

    class Factory(
        private val repository: HealthRepository,
        private val prefs: UserPreferencesRepository,
        private val sensorManager: ActivitySensorManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutViewModel(repository, prefs, sensorManager) as T
        }
    }
}
