package com.example

import android.app.Application
import com.example.data.healthconnect.HealthConnectManager
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.HealthRepository
import com.example.data.sensors.ActivitySensorManager

class MyHealthApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val sensorManager: ActivitySensorManager by lazy { ActivitySensorManager(this) }
    val healthConnectManager: HealthConnectManager by lazy { HealthConnectManager(this) }
    val healthRepository: HealthRepository by lazy {
        HealthRepository(
            dao = database.healthDao(),
            prefs = preferencesRepository,
            healthConnectManager = healthConnectManager
        )
    }
}
