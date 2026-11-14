package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.model.MealType
import com.example.ui.model.MetricType
import com.example.ui.model.TodayCardType
import com.example.ui.theme.PastelPalette
import com.example.ui.viewmodel.SensorUiState

enum class LogEntryTarget {
    STEPS_ACTIVITY,
    WATER,
    WEIGHT,
    SLEEP,
    HEART_RATE,
    NUTRITION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogHubSheet(
    onDismiss: () -> Unit,
    onSelectTarget: (LogEntryTarget) -> Unit,
    onOpenWorkouts: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Log Health Data",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            LogSheetRow(
                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                tint = PastelPalette.BlueAccent,
                title = "Steps & Activity",
                subtitle = "Log manual steps, active minutes, or distance",
                testTag = "log_option_activity",
                onClick = { onSelectTarget(LogEntryTarget.STEPS_ACTIVITY) }
            )
            LogSheetRow(
                icon = Icons.Rounded.WaterDrop,
                tint = PastelPalette.CyanAccent,
                title = "Water Intake",
                subtitle = "Add 250ml, 500ml, or custom hydration",
                testTag = "log_option_water",
                onClick = { onSelectTarget(LogEntryTarget.WATER) }
            )
            LogSheetRow(
                icon = Icons.Rounded.Restaurant,
                tint = PastelPalette.AmberAccent,
                title = "Food & Nutrition",
                subtitle = "Log meal calories, carbs, fat, and protein",
                testTag = "log_option_nutrition",
                onClick = { onSelectTarget(LogEntryTarget.NUTRITION) }
            )
            LogSheetRow(
                icon = Icons.Rounded.MonitorWeight,
                tint = PastelPalette.PurpleAccent,
                title = "Weight",
                subtitle = "Record body weight in kg",
                testTag = "log_option_weight",
                onClick = { onSelectTarget(LogEntryTarget.WEIGHT) }
            )
            LogSheetRow(
                icon = Icons.Rounded.Bedtime,
                tint = PastelPalette.PurpleAccent,
                title = "Sleep Duration",
                subtitle = "Log hours & minutes slept",
                testTag = "log_option_sleep",
                onClick = { onSelectTarget(LogEntryTarget.SLEEP) }
            )
            LogSheetRow(
                icon = Icons.Rounded.Favorite,
                tint = PastelPalette.RoseAccent,
                title = "Heart Rate",
                subtitle = "Measure via compatible sensor or enter BPM",
                testTag = "log_option_heart_rate",
                onClick = { onSelectTarget(LogEntryTarget.HEART_RATE) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            LogSheetRow(
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                tint = PastelPalette.MintAccent,
                title = "Track Live Workout",
                subtitle = "Start Walking, Running, Cycling, Gym, and more",
                testTag = "log_option_workout",
                onClick = {
                    onDismiss()
                    onOpenWorkouts()
                }
            )
        }
    }
}

@Composable
private fun LogSheetRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricIconChip(icon = icon, tint = tint)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LogActivityDialog(
    onDismiss: () -> Unit,
    onSave: (steps: Int, activeMinutes: Int, caloriesEst: Int?, distanceKm: Float?) -> Unit
) {
    var stepsText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var distanceText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Steps & Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it; errorMsg = null },
                    label = { Text("Steps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_steps")
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it; errorMsg = null },
                    label = { Text("Active Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_active_minutes")
                )
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("Distance km (optional, auto-est. if empty)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories kcal (optional, auto-est. if empty)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = stepsText.toIntOrNull() ?: 0
                    val mins = minutesText.toIntOrNull() ?: 0
                    if (steps <= 0 && mins <= 0) {
                        errorMsg = "Enter steps or active minutes."
                    } else {
                        onSave(
                            steps.coerceAtLeast(0),
                            mins.coerceAtLeast(0),
                            caloriesText.toIntOrNull(),
                            distanceText.toFloatOrNull()
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_activity_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LogWeightDialog(
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it; errorMsg = null },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_weight")
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kg = weightText.toFloatOrNull()
                    if (kg == null || kg !in 20f..350f) {
                        errorMsg = "Enter a valid weight between 20 and 350 kg."
                    } else {
                        onSave(kg)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_weight_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LogSleepDialog(
    onDismiss: () -> Unit,
    onSave: (durationMinutes: Int, bedtime: String, wakeTime: String, quality: String) -> Unit
) {
    var hoursText by remember { mutableStateOf("7") }
    var minutesText by remember { mutableStateOf("30") }
    var quality by remember { mutableStateOf("Restful") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Sleep Duration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it; errorMsg = null },
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_sleep_hours")
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { minutesText = it; errorMsg = null },
                        label = { Text("Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_sleep_minutes")
                    )
                }
                Text("Sleep Quality", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Restful", "Fair", "Light").forEach { opt ->
                        FilterChip(
                            selected = quality == opt,
                            onClick = { quality = opt },
                            label = { Text(opt) }
                        )
                    }
                }
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val total = h * 60 + m
                    if (total !in 15..1440) {
                        errorMsg = "Enter a realistic sleep duration."
                    } else {
                        onSave(total, "", "", quality)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_sleep_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LogHeartRateDialog(
    sensorState: SensorUiState,
    onRequestBodySensorsPermission: () -> Unit,
    onStartSensorMeasure: () -> Unit,
    onStopSensorMeasure: (save: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onSaveManual: (Int) -> Unit
) {
    var bpmText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (sensorState.isMeasuringHeartRate) onStopSensorMeasure(false)
            onDismiss()
        },
        title = { Text("Heart Rate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Sensors, contentDescription = null, tint = PastelPalette.RoseAccent)
                            Text("On-Device Heart Rate Sensor", style = MaterialTheme.typography.titleSmall)
                        }
                        if (!sensorState.hasHeartRateSensor) {
                            Text(
                                text = "No compatible hardware heart rate sensor found on this device. You can log BPM manually or sync from Health Connect.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!sensorState.hasBodySensorsPermission) {
                            Text(
                                text = "Body Sensors permission is required to read your device's heart rate sensor.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(onClick = onRequestBodySensorsPermission) {
                                Text("Grant Sensor Permission")
                            }
                        } else if (sensorState.isMeasuringHeartRate) {
                            Text(
                                text = sensorState.liveSensorBpm?.let { "Live Reading: $it bpm" }
                                    ?: "Reading compatible sensor… keep steady.",
                                style = MaterialTheme.typography.titleMedium,
                                color = PastelPalette.RoseAccent
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onStopSensorMeasure(true)
                                        onDismiss()
                                    },
                                    enabled = sensorState.liveSensorBpm != null
                                ) { Text("Save Reading") }
                                TextButton(onClick = { onStopSensorMeasure(false) }) { Text("Stop") }
                            }
                        } else {
                            OutlinedButton(onClick = onStartSensorMeasure) {
                                Text("Read Sensor Now")
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it; errorMsg = null },
                    label = { Text("Manual Heart Rate (bpm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_heart_rate")
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bpm = bpmText.toIntOrNull()
                    if (bpm == null || bpm !in 30..240) {
                        errorMsg = "Enter BPM between 30 and 240."
                    } else {
                        onSaveManual(bpm)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_heart_rate_button")
            ) { Text("Save Manual") }
        },
        dismissButton = {
            TextButton(onClick = {
                if (sensorState.isMeasuringHeartRate) onStopSensorMeasure(false)
                onDismiss()
            }) { Text("Close") }
        }
    )
}

@Composable
fun LogWaterDialog(
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var mlText by remember { mutableStateOf("250") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Water Intake") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(250, 500, 750).forEach { preset ->
                        FilterChip(
                            selected = mlText == preset.toString(),
                            onClick = { mlText = preset.toString(); errorMsg = null },
                            label = { Text("${preset}ml") }
                        )
                    }
                }
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { mlText = it; errorMsg = null },
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_water_ml")
                )
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ml = mlText.toIntOrNull()
                    if (ml == null || ml !in 25..5000) {
                        errorMsg = "Enter between 25 and 5000 ml."
                    } else {
                        onSave(ml)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_water_button")
            ) { Text("Add Water") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogNutritionDialog(
    onDismiss: () -> Unit,
    onSave: (mealName: String, mealType: String, calories: Int, carbs: Float, fat: Float, protein: Float) -> Unit
) {
    var mealName by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var caloriesText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Food & Nutrition") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it; errorMsg = null },
                    label = { Text("Food or Meal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_meal_name")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MealType.entries.forEach { type ->
                        FilterChip(
                            selected = mealType == type,
                            onClick = { mealType = type },
                            label = { Text(type.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it; errorMsg = null },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_meal_calories")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = caloriesText.toIntOrNull()
                    if (mealName.isBlank() || cal == null || cal < 0) {
                        errorMsg = "Enter a meal name and valid calories."
                    } else {
                        onSave(
                            mealName.trim(),
                            mealType.displayName,
                            cal,
                            carbsText.toFloatOrNull() ?: 0f,
                            fatText.toFloatOrNull() ?: 0f,
                            proteinText.toFloatOrNull() ?: 0f
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_nutrition_button")
            ) { Text("Save Meal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeHealthMetricsSheet(
    visibleMetricIds: Set<String>,
    onToggleMetric: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Customize Health Metrics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose which metric cards appear on your Health grid.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            MetricType.entries.forEach { metric ->
                val checked = metric.id in visibleMetricIds
                ListItem(
                    headlineContent = { Text(metric.titleEn) },
                    leadingContent = {
                        Icon(metricIcon(metric), contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleMetric(metric.id) }
                        )
                    },
                    modifier = Modifier.clickable { onToggleMetric(metric.id) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeTodaySheet(
    visibleCardIds: Set<String>,
    currentStepGoal: Int,
    currentExerciseGoal: Int,
    currentCardioGoal: Int,
    onToggleCard: (String) -> Unit,
    onSaveGoals: (steps: Int, exerciseMin: Int, cardioMin: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var stepGoalText by remember { mutableStateOf(currentStepGoal.toString()) }
    var exerciseGoalText by remember { mutableStateOf(currentExerciseGoal.toString()) }
    var cardioGoalText by remember { mutableStateOf(currentCardioGoal.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Edit Today Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Activity Goals",
                style = MaterialTheme.typography.titleMedium
            )
            OutlinedTextField(
                value = stepGoalText,
                onValueChange = { stepGoalText = it },
                label = { Text("Daily Step Goal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = exerciseGoalText,
                    onValueChange = { exerciseGoalText = it },
                    label = { Text("Daily Exercise (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cardioGoalText,
                    onValueChange = { cardioGoalText = it },
                    label = { Text("Weekly Cardio (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Visible Health Cards",
                style = MaterialTheme.typography.titleMedium
            )
            TodayCardType.entries.forEach { card ->
                val checked = card.id in visibleCardIds
                ListItem(
                    headlineContent = { Text(card.title) },
                    leadingContent = {
                        Icon(todayCardIcon(card), contentDescription = null)
                    },
                    trailingContent = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleCard(card.id) }
                        )
                    },
                    modifier = Modifier.clickable { onToggleCard(card.id) }
                )
            }
            Button(
                onClick = {
                    onSaveGoals(
                        stepGoalText.toIntOrNull() ?: currentStepGoal,
                        exerciseGoalText.toIntOrNull() ?: currentExerciseGoal,
                        cardioGoalText.toIntOrNull() ?: currentCardioGoal
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    }
}
