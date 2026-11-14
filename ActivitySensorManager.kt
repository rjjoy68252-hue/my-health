package com.example.data.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ActivitySensorManager(private val context: Context) {

    private val sensorManager: SensorManager? =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    val hasStepCounterSensor: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null

    val hasHeartRateSensor: Boolean
        get() = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBodySensorsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Emits cumulative reboot step counter readings from TYPE_STEP_COUNTER, or 1f per step
     * event tagged via Pair(value, isCumulativeCounter).
     */
    fun observeStepSensorEvents(): Flow<Pair<Float, Boolean>> = callbackFlow {
        if (sensorManager == null || !hasActivityRecognitionPermission()) {
            close()
            return@callbackFlow
        }

        val stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val stepDetector = if (stepCounter == null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        } else null

        val activeSensor = stepCounter ?: stepDetector
        if (activeSensor == null) {
            close()
            return@callbackFlow
        }

        val isCumulative = stepCounter != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val value = event?.values?.firstOrNull() ?: return
                if (value >= 0f) {
                    trySend(value to isCumulative)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, activeSensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Emits real hardware heart rate BPM readings only if a compatible TYPE_HEART_RATE sensor exists
     * and BODY_SENSORS permission is granted. Never generates simulated BPM values.
     */
    fun observeHeartRateSensor(): Flow<Int> = callbackFlow {
        if (sensorManager == null || !hasHeartRateSensor || !hasBodySensorsPermission()) {
            close()
            return@callbackFlow
        }
        val hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        if (hrSensor == null) {
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val bpm = event?.values?.firstOrNull()?.toInt() ?: return
                if (bpm in 30..240) {
                    trySend(bpm)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /**
     * Emits incremental distance in meters from GPS/Network location providers during outdoor workouts.
     */
    @SuppressLint("MissingPermission")
    fun observeWorkoutDistanceMeters(): Flow<Float> = callbackFlow {
        if (locationManager == null || !hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        var lastLocation: Location? = null

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Filter out noisy low-accuracy spikes
                if (location.hasAccuracy() && location.accuracy > 45f) return
                val previous = lastLocation
                if (previous != null) {
                    val deltaMeters = previous.distanceTo(location)
                    if (deltaMeters in 1.5f..150f) {
                        trySend(deltaMeters)
                    }
                }
                lastLocation = location
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val providers = buildList {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        if (providers.isEmpty()) {
            close()
            return@callbackFlow
        }

        providers.forEach { provider ->
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    3000L,
                    2f,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {
                // Handled gracefully if permission was revoked
            }
        }

        awaitClose {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {
            }
        }
    }
}
