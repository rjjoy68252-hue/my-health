package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.navigation.MyHealthApp
import com.example.ui.theme.MyHealthTheme
import com.example.ui.viewmodel.HealthViewModel
import com.example.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    private val app by lazy { application as MyHealthApplication }

    private val healthViewModel: HealthViewModel by viewModels {
        HealthViewModel.Factory(
            repository = app.healthRepository,
            prefs = app.preferencesRepository,
            sensorManager = app.sensorManager
        )
    }

    private val workoutViewModel: WorkoutViewModel by viewModels {
        WorkoutViewModel.Factory(
            repository = app.healthRepository,
            prefs = app.preferencesRepository,
            sensorManager = app.sensorManager
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val rawSnapshot by healthViewModel.rawSnapshot.collectAsStateWithLifecycle()
            MyHealthTheme(darkThemeMode = rawSnapshot.settings.darkMode) {
                MyHealthApp(
                    healthViewModel = healthViewModel,
                    workoutViewModel = workoutViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        healthViewModel.refreshSensorPermissions()
        healthViewModel.refreshHealthConnectStatus()
    }
}
