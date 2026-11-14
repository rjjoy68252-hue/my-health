package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import com.example.R
import com.example.data.preferences.UserProfileAndSettings
import com.example.ui.components.WellnessDisclaimerBanner
import com.example.ui.model.HealthConnectStatus
import com.example.ui.theme.PastelPalette
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    settings: UserProfileAndSettings,
    latestWeightKg: Float?,
    healthConnectStatus: HealthConnectStatus,
    healthConnectPermissions: Set<String>,
    onUpdateProfile: (name: String, age: Int?, heightCm: Float?, gender: String) -> Unit,
    onLogWeight: (Float) -> Unit,
    onUpdateGoals: (steps: Int, waterMl: Int, exerciseMin: Int, cardioMin: Int, calGoal: Int) -> Unit,
    onSetDarkMode: (String) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetWorkoutReminders: (Boolean) -> Unit,
    onSetHydrationReminders: (Boolean) -> Unit,
    onSetHealthConnectSync: (Boolean) -> Unit,
    onSetSensorSteps: (Boolean) -> Unit,
    onSyncHealthConnect: () -> Unit,
    onHealthConnectPermissionsResult: () -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditGoalsDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        onHealthConnectPermissionsResult()
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSetWorkoutReminders(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Profile Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_profile_avatar),
                                contentDescription = "User avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.name.ifBlank { "My Health Profile" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                val details = buildList {
                                    settings.age?.let { add("$it yrs") }
                                    settings.heightCm?.let { add("${it.toInt()} cm") }
                                    latestWeightKg?.let { add(String.format(Locale.US, "%.1f kg", it)) }
                                    if (settings.gender.isNotBlank()) add(settings.gender)
                                }
                                Text(
                                    text = if (details.isEmpty()) "Tap Edit to add age, height, weight" else details.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Edit")
                            }
                        }
                    }
                }
            }

            // Goals Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fitness & Hydration Goals", style = MaterialTheme.typography.titleMedium)
                            TextButton(
                                onClick = { showEditGoalsDialog = true },
                                modifier = Modifier.testTag("edit_goals_button")
                            ) { Text("Update") }
                        }
                        GoalSummaryRow("Daily Step Goal", "${settings.dailyStepGoal} steps")
                        GoalSummaryRow("Daily Water Goal", "${settings.dailyWaterGoalMl} ml")
                        GoalSummaryRow("Daily Exercise Goal", "${settings.dailyExerciseGoalMin} min")
                        GoalSummaryRow("Weekly Cardio Goal", "${settings.weeklyCardioGoalMin} min")
                        GoalSummaryRow("Daily Calorie Goal", "${settings.dailyCalorieIntakeGoal} kcal")
                    }
                }
            }

            // Health Connect Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Google Health Connect (Optional)", style = MaterialTheme.typography.titleMedium)
                        }
                        val statusDesc = when {
                            !healthConnectStatus.isSupportedDevice -> "Requires Android 9+ (this device uses local Room storage & sensors)."
                            healthConnectStatus.needsProviderUpdate -> "Health Connect app update is needed to enable sync."
                            !healthConnectStatus.isAvailable -> "Health Connect is not installed on this device. All data stays safely stored locally."
                            healthConnectStatus.hasPermissions -> "Connected. You can sync steps, HR, sleep, weight, and hydration."
                            else -> "Available on this device. Grant permissions to sync with Health Connect."
                        }
                        Text(
                            text = statusDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        healthConnectStatus.lastSyncMessage?.let { msg ->
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (healthConnectStatus.isAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { hcPermissionLauncher.launch(healthConnectPermissions) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (healthConnectStatus.hasPermissions) "Permissions" else "Connect")
                                }
                                Button(
                                    onClick = onSyncHealthConnect,
                                    enabled = healthConnectStatus.hasPermissions,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Sync Now")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto-write new logs to Health Connect", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = settings.healthConnectSyncEnabled,
                                    onCheckedChange = onSetHealthConnectSync
                                )
                            }
                        }
                    }
                }
            }

            // Notifications, Sensors, Appearance & Language
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Sensors & Notifications", style = MaterialTheme.typography.titleMedium)
                        }
                        SettingSwitchRow(
                            label = "Device step sensor tracking",
                            checked = settings.sensorStepTrackingEnabled,
                            onCheckedChange = onSetSensorSteps
                        )
                        SettingSwitchRow(
                            label = "Workout reminders",
                            checked = settings.workoutRemindersEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetWorkoutReminders(enable)
                                }
                            }
                        )
                        SettingSwitchRow(
                            label = "Hydration reminders",
                            checked = settings.hydrationRemindersEnabled,
                            onCheckedChange = { enable ->
                                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSetHydrationReminders(enable)
                            }
                        )

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Dark Mode", style = MaterialTheme.typography.titleMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("System", "Light", "Dark").forEach { mode ->
                                FilterChip(
                                    selected = settings.darkMode == mode,
                                    onClick = { onSetDarkMode(mode) },
                                    label = { Text(mode) },
                                    modifier = Modifier.testTag("theme_chip_${mode.lowercase()}")
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Language", style = MaterialTheme.typography.titleMedium)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("English", "Spanish", "French", "German").forEach { lang ->
                                FilterChip(
                                    selected = settings.language == lang,
                                    onClick = { onSetLanguage(lang) },
                                    label = { Text(lang) }
                                )
                            }
                        }
                    }
                }
            }

            // Data Privacy & Safety
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, tint = PastelPalette.MintAccent)
                            Text("Data Privacy & Safety", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = stringResource(R.string.privacy_notice_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PastelPalette.RoseAccent),
                            modifier = Modifier.fillMaxWidth().testTag("clear_local_data_button")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Clear All Local Health Data")
                        }
                    }
                }
            }

            item {
                WellnessDisclaimerBanner()
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            settings = settings,
            latestWeightKg = latestWeightKg,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, age, height, gender, weight ->
                onUpdateProfile(name, age, height, gender)
                if (weight != null && weight in 20f..350f) {
                    onLogWeight(weight)
                }
                showEditProfileDialog = false
            }
        )
    }

    if (showEditGoalsDialog) {
        EditGoalsDialog(
            settings = settings,
            onDismiss = { showEditGoalsDialog = false },
            onSave = { steps, water, exercise, cardio, cals ->
                onUpdateGoals(steps, water, exercise, cardio, cals)
                showEditGoalsDialog = false
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Local Health Data?") },
            text = { Text("This permanently removes all locally saved steps, workouts, sleep, heart rate, water, weight, and nutrition records from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelPalette.RoseAccent)
                ) { Text("Clear Data") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GoalSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    settings: UserProfileAndSettings,
    latestWeightKg: Float?,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int?, heightCm: Float?, gender: String, weightKg: Float?) -> Unit
) {
    var name by remember { mutableStateOf(settings.name) }
    var ageText by remember { mutableStateOf(settings.age?.toString() ?: "") }
    var heightText by remember { mutableStateOf(settings.heightCm?.toInt()?.toString() ?: "") }
    var weightText by remember { mutableStateOf(latestWeightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "") }
    var gender by remember { mutableStateOf(settings.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Current Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Gender (Optional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("", "Female", "Male", "Non-binary").forEach { opt ->
                        FilterChip(
                            selected = gender == opt,
                            onClick = { gender = opt },
                            label = { Text(opt.ifBlank { "Prefer not to say" }) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        ageText.toIntOrNull(),
                        heightText.toFloatOrNull(),
                        gender,
                        weightText.toFloatOrNull()
                    )
                },
                modifier = Modifier.testTag("save_profile_button")
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditGoalsDialog(
    settings: UserProfileAndSettings,
    onDismiss: () -> Unit,
    onSave: (steps: Int, waterMl: Int, exerciseMin: Int, cardioMin: Int, cals: Int) -> Unit
) {
    var steps by remember { mutableStateOf(settings.dailyStepGoal.toString()) }
    var water by remember { mutableStateOf(settings.dailyWaterGoalMl.toString()) }
    var exercise by remember { mutableStateOf(settings.dailyExerciseGoalMin.toString()) }
    var cardio by remember { mutableStateOf(settings.weeklyCardioGoalMin.toString()) }
    var cals by remember { mutableStateOf(settings.dailyCalorieIntakeGoal.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Daily & Weekly Goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = steps,
                    onValueChange = { steps = it },
                    label = { Text("Daily Step Goal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = water,
                    onValueChange = { water = it },
                    label = { Text("Daily Water Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exercise,
                    onValueChange = { exercise = it },
                    label = { Text("Daily Exercise Goal (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardio,
                    onValueChange = { cardio = it },
                    label = { Text("Weekly Cardio Goal (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cals,
                    onValueChange = { cals = it },
                    label = { Text("Daily Calorie Intake Goal (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    steps.toIntOrNull() ?: settings.dailyStepGoal,
                    water.toIntOrNull() ?: settings.dailyWaterGoalMl,
                    exercise.toIntOrNull() ?: settings.dailyExerciseGoalMin,
                    cardio.toIntOrNull() ?: settings.weeklyCardioGoalMin,
                    cals.toIntOrNull() ?: settings.dailyCalorieIntakeGoal
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
