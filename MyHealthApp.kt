package com.example.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.CustomizeHealthMetricsSheet
import com.example.ui.components.CustomizeTodaySheet
import com.example.ui.components.LogActivityDialog
import com.example.ui.components.LogEntryTarget
import com.example.ui.components.LogHeartRateDialog
import com.example.ui.components.LogNutritionDialog
import com.example.ui.components.LogSleepDialog
import com.example.ui.components.LogWaterDialog
import com.example.ui.components.LogWeightDialog
import com.example.ui.components.QuickLogHubSheet
import com.example.ui.model.MetricType
import com.example.ui.model.WellnessStrings
import com.example.ui.screens.HealthMetricsScreen
import com.example.ui.screens.MetricDetailScreen
import com.example.ui.screens.ProfileSettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.viewmodel.HealthViewModel
import com.example.ui.viewmodel.WorkoutViewModel

@Composable
fun MyHealthApp(
    healthViewModel: HealthViewModel,
    workoutViewModel: WorkoutViewModel
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    val rawSnapshot by healthViewModel.rawSnapshot.collectAsStateWithLifecycle()
    val todayState by healthViewModel.todayState.collectAsStateWithLifecycle()
    val metricCards by healthViewModel.metricCards.collectAsStateWithLifecycle()
    val sensorState by healthViewModel.sensorState.collectAsStateWithLifecycle()
    val hcStatus by healthViewModel.healthConnectStatus.collectAsStateWithLifecycle()
    val statusMessage by healthViewModel.statusMessage.collectAsStateWithLifecycle()

    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val workoutHistory by workoutViewModel.workoutHistory.collectAsStateWithLifecycle()
    val workoutSavedMessage by workoutViewModel.savedMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var showQuickLogSheet by remember { mutableStateOf(false) }
    var activeLogDialog by remember { mutableStateOf<LogEntryTarget?>(null) }
    var showCustomizeToday by remember { mutableStateOf(false) }
    var showCustomizeHealth by remember { mutableStateOf(false) }

    val activityPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        healthViewModel.refreshSensorPermissions()
    }

    val bodySensorPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        healthViewModel.refreshSensorPermissions()
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            healthViewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(workoutSavedMessage) {
        workoutSavedMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            workoutViewModel.clearSavedMessage()
        }
    }

    val isTopLevelTab = currentDest?.hasRoute<TodayRoute>() == true ||
        currentDest?.hasRoute<HealthRoute>() == true

    val lang = rawSnapshot.settings.language

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isTopLevelTab) {
                val isTodaySelected = currentDest?.hasRoute<TodayRoute>() == true
                val isHealthSelected = currentDest?.hasRoute<HealthRoute>() == true

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    NavigationBarItem(
                        selected = isTodaySelected,
                        onClick = {
                            navController.navigate(TodayRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isTodaySelected) Icons.Rounded.Today else Icons.Outlined.Today,
                                contentDescription = WellnessStrings.tabToday(lang)
                            )
                        },
                        label = { Text(WellnessStrings.tabToday(lang)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_today")
                    )
                    NavigationBarItem(
                        selected = isHealthSelected,
                        onClick = {
                            navController.navigate(HealthRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isHealthSelected) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = WellnessStrings.tabHealth(lang)
                            )
                        },
                        label = { Text(WellnessStrings.tabHealth(lang)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_health")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = TodayRoute,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) }
            ) {
                composable<TodayRoute> {
                    TodayScreen(
                        todayState = todayState,
                        settings = rawSnapshot.settings,
                        sensorState = sensorState,
                        healthConnectStatus = hcStatus,
                        onRequestStepPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                activityPermLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                healthViewModel.refreshSensorPermissions()
                            }
                        },
                        onOpenLogSheet = { showQuickLogSheet = true },
                        onOpenWorkouts = { navController.navigate(WorkoutsRoute) },
                        onOpenEditToday = { showCustomizeToday = true },
                        onOpenMetricDetail = { metric ->
                            navController.navigate(MetricDetailRoute(metric.id))
                        },
                        onQuickAddWater = { ml -> healthViewModel.logWater(ml) },
                        onOpenNutritionLog = { activeLogDialog = LogEntryTarget.NUTRITION },
                        onOpenProfile = { navController.navigate(ProfileSettingsRoute) },
                        onSyncHealthConnect = { healthViewModel.syncHealthConnectNow() }
                    )
                }

                composable<HealthRoute> {
                    HealthMetricsScreen(
                        metricCards = metricCards,
                        settings = rawSnapshot.settings,
                        onEditMetricsClick = { showCustomizeHealth = true },
                        onCardClick = { metric ->
                            navController.navigate(MetricDetailRoute(metric.id))
                        },
                        onOpenProfile = { navController.navigate(ProfileSettingsRoute) }
                    )
                }

                composable<WorkoutsRoute> {
                    WorkoutScreen(
                        activeWorkout = activeWorkout,
                        workoutHistory = workoutHistory,
                        hasLocationPermission = workoutViewModel.hasLocationPermission(),
                        hasActivityPermission = workoutViewModel.hasActivityPermission(),
                        onSelectCategory = { workoutViewModel.selectCategory(it) },
                        onStartWorkout = { workoutViewModel.startWorkout() },
                        onTogglePause = { workoutViewModel.togglePauseResume() },
                        onStopAndSave = { workoutViewModel.stopAndSaveWorkout() },
                        onDiscard = { workoutViewModel.discardWorkout() },
                        onDeleteHistory = { workoutViewModel.deleteHistoryItem(it) },
                        onPermissionsUpdated = { healthViewModel.refreshSensorPermissions() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<MetricDetailRoute> { entry ->
                    val route = entry.toRoute<MetricDetailRoute>()
                    val metricType = MetricType.entries.firstOrNull { it.id == route.metricId } ?: MetricType.STEPS
                    MetricDetailScreen(
                        metricType = metricType,
                        snapshot = rawSnapshot,
                        getChartPoints = { t, r -> healthViewModel.getMetricChartPoints(t, r) },
                        onLogMetricClick = { type ->
                            activeLogDialog = when (type) {
                                MetricType.WEIGHT -> LogEntryTarget.WEIGHT
                                MetricType.ENERGY_BURNED, MetricType.STEPS, MetricType.WALKING_DISTANCE -> LogEntryTarget.STEPS_ACTIVITY
                                MetricType.CALORIES_INTAKE, MetricType.CARBS, MetricType.FAT, MetricType.PROTEIN -> LogEntryTarget.NUTRITION
                                MetricType.HEART_RATE -> LogEntryTarget.HEART_RATE
                                MetricType.SLEEP_DURATION -> LogEntryTarget.SLEEP
                                MetricType.WATER_INTAKE -> LogEntryTarget.WATER
                                MetricType.EXERCISE_DAYS -> null
                            }
                            if (type == MetricType.EXERCISE_DAYS) {
                                navController.navigate(WorkoutsRoute)
                            }
                        },
                        onDeleteWeight = { healthViewModel.deleteWeight(it) },
                        onDeleteSleep = { healthViewModel.deleteSleep(it) },
                        onDeleteHeartRate = { healthViewModel.deleteHeartRate(it) },
                        onDeleteWater = { healthViewModel.deleteWater(it) },
                        onDeleteNutrition = { healthViewModel.deleteNutrition(it) },
                        onDeleteWorkout = { healthViewModel.deleteWorkout(it) },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable<ProfileSettingsRoute> {
                    ProfileSettingsScreen(
                        settings = rawSnapshot.settings,
                        latestWeightKg = rawSnapshot.weights.firstOrNull()?.weightKg,
                        healthConnectStatus = hcStatus,
                        healthConnectPermissions = healthViewModel.let {
                            (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.MyHealthApplication)
                                .healthConnectManager.requiredPermissions
                        },
                        onUpdateProfile = { name, age, height, gender ->
                            healthViewModel.updateProfile(name, age, height, gender)
                        },
                        onLogWeight = { healthViewModel.logWeight(it) },
                        onUpdateGoals = { steps, water, exercise, cardio, cals ->
                            healthViewModel.updateGoals(steps, water, exercise, cardio, cals)
                        },
                        onSetDarkMode = { healthViewModel.setDarkMode(it) },
                        onSetLanguage = { healthViewModel.setLanguage(it) },
                        onSetWorkoutReminders = { healthViewModel.setWorkoutReminders(it) },
                        onSetHydrationReminders = { healthViewModel.setHydrationReminders(it) },
                        onSetHealthConnectSync = { healthViewModel.setHealthConnectSync(it) },
                        onSetSensorSteps = { healthViewModel.setSensorStepTracking(it) },
                        onSyncHealthConnect = { healthViewModel.syncHealthConnectNow() },
                        onHealthConnectPermissionsResult = { healthViewModel.refreshHealthConnectStatus() },
                        onClearAllData = { healthViewModel.clearAllLocalData() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showQuickLogSheet) {
        QuickLogHubSheet(
            onDismiss = { showQuickLogSheet = false },
            onSelectTarget = { target ->
                showQuickLogSheet = false
                activeLogDialog = target
            },
            onOpenWorkouts = { navController.navigate(WorkoutsRoute) }
        )
    }

    when (activeLogDialog) {
        LogEntryTarget.STEPS_ACTIVITY -> LogActivityDialog(
            onDismiss = { activeLogDialog = null },
            onSave = { steps, mins, cals, dist ->
                healthViewModel.logStepsAndActivity(steps, mins, cals, dist)
            }
        )
        LogEntryTarget.WEIGHT -> LogWeightDialog(
            onDismiss = { activeLogDialog = null },
            onSave = { kg -> healthViewModel.logWeight(kg) }
        )
        LogEntryTarget.SLEEP -> LogSleepDialog(
            onDismiss = { activeLogDialog = null },
            onSave = { mins, bed, wake, qual ->
                healthViewModel.logSleep(mins, bed, wake, qual)
            }
        )
        LogEntryTarget.HEART_RATE -> LogHeartRateDialog(
            sensorState = sensorState,
            onRequestBodySensorsPermission = {
                bodySensorPermLauncher.launch(Manifest.permission.BODY_SENSORS)
            },
            onStartSensorMeasure = { healthViewModel.startHeartRateSensorMeasurement() },
            onStopSensorMeasure = { save -> healthViewModel.stopHeartRateSensorMeasurement(save) },
            onDismiss = { activeLogDialog = null },
            onSaveManual = { bpm -> healthViewModel.logHeartRate(bpm) }
        )
        LogEntryTarget.WATER -> LogWaterDialog(
            onDismiss = { activeLogDialog = null },
            onSave = { ml -> healthViewModel.logWater(ml) }
        )
        LogEntryTarget.NUTRITION -> LogNutritionDialog(
            onDismiss = { activeLogDialog = null },
            onSave = { name, type, cals, carbs, fat, protein ->
                healthViewModel.logNutrition(name, type, cals, carbs, fat, protein)
            }
        )
        null -> Unit
    }

    if (showCustomizeToday) {
        CustomizeTodaySheet(
            visibleCardIds = rawSnapshot.settings.visibleTodayCardIds,
            currentStepGoal = rawSnapshot.settings.dailyStepGoal,
            currentExerciseGoal = rawSnapshot.settings.dailyExerciseGoalMin,
            currentCardioGoal = rawSnapshot.settings.weeklyCardioGoalMin,
            onToggleCard = { healthViewModel.toggleTodayCardVisibility(it) },
            onSaveGoals = { steps, exMin, cardioMin ->
                healthViewModel.updateGoals(
                    dailySteps = steps,
                    dailyWaterMl = rawSnapshot.settings.dailyWaterGoalMl,
                    dailyExerciseMin = exMin,
                    weeklyCardioMin = cardioMin,
                    dailyCalorieGoal = rawSnapshot.settings.dailyCalorieIntakeGoal
                )
            },
            onDismiss = { showCustomizeToday = false }
        )
    }

    if (showCustomizeHealth) {
        CustomizeHealthMetricsSheet(
            visibleMetricIds = rawSnapshot.settings.visibleMetricIds,
            onToggleMetric = { healthViewModel.toggleMetricVisibility(it) },
            onDismiss = { showCustomizeHealth = false }
        )
    }
}
