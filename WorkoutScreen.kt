package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.WorkoutEntity
import com.example.ui.components.MetricIconChip
import com.example.ui.components.WellnessDisclaimerBanner
import com.example.ui.components.workoutCategoryIcon
import com.example.ui.model.ActiveWorkoutState
import com.example.ui.model.WorkoutCategory
import com.example.ui.model.formatStopwatch
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.PastelPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    activeWorkout: ActiveWorkoutState,
    workoutHistory: List<WorkoutEntity>,
    hasLocationPermission: Boolean,
    hasActivityPermission: Boolean,
    onSelectCategory: (WorkoutCategory) -> Unit,
    onStartWorkout: () -> Unit,
    onTogglePause: () -> Unit,
    onStopAndSave: () -> Unit,
    onDiscard: () -> Unit,
    onDeleteHistory: (Long) -> Unit,
    onPermissionsUpdated: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isDark = LocalIsDarkTheme.current
    var pendingOutdoorStart by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionsUpdated()
        if (pendingOutdoorStart) {
            pendingOutdoorStart = false
            onStartWorkout()
        }
    }

    fun handleStartPressed() {
        val cat = activeWorkout.category
        val missingPerms = buildList {
            if (cat.tracksDistance && !hasLocationPermission) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasActivityPermission) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (missingPerms.isNotEmpty()) {
            pendingOutdoorStart = true
            permissionLauncher.launch(missingPerms.toTypedArray())
        } else {
            onStartWorkout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workouts") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("workout_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Workout Category Selector
            item {
                Text(
                    text = "Exercise Type",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WorkoutCategory.entries.forEach { category ->
                        FilterChip(
                            selected = activeWorkout.category == category,
                            onClick = { onSelectCategory(category) },
                            enabled = !activeWorkout.isRunning,
                            label = { Text(category.displayName) },
                            leadingIcon = {
                                Icon(
                                    workoutCategoryIcon(category),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("workout_type_${category.id}")
                        )
                    }
                }
            }

            // Live Workout Console Card
            item {
                val cardBg = if (isDark) PastelPalette.BlueCardDark else PastelPalette.BlueCardLight
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, cardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricIconChip(
                                icon = workoutCategoryIcon(activeWorkout.category),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = activeWorkout.category.displayName,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        // Stopwatch Timer
                        Text(
                            text = formatStopwatch(activeWorkout.elapsedSeconds),
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("workout_timer_text")
                        )

                        // Live Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WorkoutStatColumn(
                                label = "Est. Calories",
                                value = "${activeWorkout.estimatedCalories} kcal"
                            )
                            if (activeWorkout.category.tracksDistance) {
                                val distLabel = if (activeWorkout.isDistanceGpsBacked) "Distance (GPS)" else "Distance (Est.)"
                                WorkoutStatColumn(
                                    label = distLabel,
                                    value = String.format(Locale.US, "%.2f km", activeWorkout.distanceKm)
                                )
                            } else {
                                WorkoutStatColumn(
                                    label = "Distance",
                                    value = "N/A"
                                )
                            }
                            WorkoutStatColumn(
                                label = "Steps",
                                value = activeWorkout.stepsTaken.toString()
                            )
                        }

                        if (activeWorkout.category.tracksDistance && !hasLocationPermission) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Location optional: GPS enables route distance; otherwise distance is estimated from steps.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Controls: Start / Pause / Stop
                        if (!activeWorkout.isRunning) {
                            Button(
                                onClick = { handleStartPressed() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("workout_start_button")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Start ${activeWorkout.category.displayName}")
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onTogglePause,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("workout_pause_button")
                                ) {
                                    Icon(
                                        imageVector = if (activeWorkout.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(if (activeWorkout.isPaused) "Resume" else "Pause")
                                }
                                Button(
                                    onClick = onStopAndSave,
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelPalette.RoseAccent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("workout_stop_button")
                                ) {
                                    Icon(Icons.Rounded.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text("Stop & Save")
                                }
                            }
                            OutlinedButton(
                                onClick = onDiscard,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Discard Session")
                            }
                        }
                    }
                }
            }

            // Workout History
            item {
                Text(
                    text = "Workout History",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (workoutHistory.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No workouts recorded yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Select an exercise above and press Start to track duration, estimated calories, and distance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(workoutHistory, key = { it.id }) { workout ->
                    WorkoutHistoryCard(
                        workout = workout,
                        onDelete = { onDeleteHistory(workout.id) }
                    )
                }
            }

            item {
                WellnessDisclaimerBanner()
            }
        }
    }
}

@Composable
private fun WorkoutStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutHistoryCard(
    workout: WorkoutEntity,
    onDelete: () -> Unit
) {
    val category = WorkoutCategory.entries.firstOrNull { it.id == workout.categoryId } ?: WorkoutCategory.OTHER
    val dateFmt = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricIconChip(icon = workoutCategoryIcon(category), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dateFmt.format(Date(workout.startTimeMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val distPart = workout.distanceKm?.let {
                    val est = if (workout.isDistanceEstimated) "Est. " else ""
                    " • ${est}${String.format(Locale.US, "%.2f", it)} km"
                } ?: ""
                Text(
                    text = "${formatStopwatch(workout.durationSeconds)} • Est. ${workout.caloriesBurnedEst} kcal$distPart",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete workout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
