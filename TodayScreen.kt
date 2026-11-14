package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserProfileAndSettings
import com.example.ui.components.ActivityDualRingCanvas
import com.example.ui.components.MetricIconChip
import com.example.ui.components.ProfileAvatarButton
import com.example.ui.components.WellnessDisclaimerBanner
import com.example.ui.components.todayCardIcon
import com.example.ui.model.HealthConnectStatus
import com.example.ui.model.MetricType
import com.example.ui.model.TodayCardType
import com.example.ui.model.TodayDashboardState
import com.example.ui.model.WellnessStrings
import com.example.ui.model.formatDurationMinutes
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.PastelPalette
import com.example.ui.viewmodel.SensorUiState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TodayScreen(
    todayState: TodayDashboardState,
    settings: UserProfileAndSettings,
    sensorState: SensorUiState,
    healthConnectStatus: HealthConnectStatus,
    onRequestStepPermission: () -> Unit,
    onOpenLogSheet: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenEditToday: () -> Unit,
    onOpenMetricDetail: (MetricType) -> Unit,
    onQuickAddWater: (Int) -> Unit,
    onOpenNutritionLog: () -> Unit,
    onOpenProfile: () -> Unit,
    onSyncHealthConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val lang = settings.language
    val noDataText = WellnessStrings.noData(lang)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 680.dp) 3 else 2

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = todayState.dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val greeting = if (settings.name.isNotBlank()) {
                            "My Health • ${settings.name}"
                        } else {
                            "My Health"
                        }
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (healthConnectStatus.isAvailable && healthConnectStatus.hasPermissions) {
                            AssistChip(
                                onClick = onSyncHealthConnect,
                                label = { Text("Sync", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Sync,
                                        contentDescription = "Sync Health Connect",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        ProfileAvatarButton(onClick = onOpenProfile)
                    }
                }
            }

            // Activity Overview Card
            item {
                ActivityOverviewCard(
                    todayState = todayState,
                    title = WellnessStrings.activityOverview(lang),
                    noDataText = noDataText,
                    sensorState = sensorState,
                    onRequestStepPermission = onRequestStepPermission
                )
            }

            // Action Buttons: Log, Start Workout, Edit
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenLogSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("today_log_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(WellnessStrings.logAction(lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = onOpenWorkouts,
                        modifier = Modifier
                            .weight(1.35f)
                            .height(48.dp)
                            .testTag("today_start_workout_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(WellnessStrings.startWorkout(lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenEditToday,
                        modifier = Modifier
                            .weight(0.95f)
                            .height(48.dp)
                            .testTag("today_edit_button"),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(WellnessStrings.editAction(lang), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // Health Cards Section Header
            item {
                Text(
                    text = "Health Cards",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 6 Pastel Health Cards in responsive grid
            val visibleCards = TodayCardType.entries.filter { it.id in settings.visibleTodayCardIds }
            val chunkedCards = visibleCards.chunked(columns)
            items(chunkedCards.size) { rowIdx ->
                val rowItems = chunkedCards[rowIdx]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { cardType ->
                        TodayHealthPastelCard(
                            cardType = cardType,
                            todayState = todayState,
                            noDataText = noDataText,
                            isDark = isDark,
                            onClick = {
                                val targetMetric = when (cardType) {
                                    TodayCardType.STEPS -> MetricType.STEPS
                                    TodayCardType.READINESS -> MetricType.SLEEP_DURATION
                                    TodayCardType.SLEEP -> MetricType.SLEEP_DURATION
                                    TodayCardType.HEART_RATE -> MetricType.HEART_RATE
                                    TodayCardType.CALORIES_BURNED -> MetricType.ENERGY_BURNED
                                    TodayCardType.DISTANCE -> MetricType.WALKING_DISTANCE
                                }
                                onOpenMetricDetail(targetMetric)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Quick Hydration & Nutrition Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hydration & Nutrition",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                val waterStr = if (todayState.waterIntakeMl > 0) {
                                    "${todayState.waterIntakeMl} / ${todayState.waterGoalMl} ml"
                                } else {
                                    "$noDataText • Goal ${todayState.waterGoalMl} ml"
                                }
                                val calStr = if (todayState.caloriesIntake > 0) {
                                    "${todayState.caloriesIntake} / ${todayState.calorieIntakeGoal} kcal"
                                } else {
                                    noDataText
                                }
                                Text(
                                    text = "Water: $waterStr  •  Intake: $calStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onQuickAddWater(250) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDark) PastelPalette.CyanCardDark else PastelPalette.CyanCardLight,
                                    contentColor = if (isDark) PastelPalette.CyanAccentDark else PastelPalette.CyanAccent
                                ),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("quick_add_water_250")
                            ) {
                                Icon(Icons.Rounded.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("+250 ml")
                            }
                            FilledTonalButton(
                                onClick = onOpenNutritionLog,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isDark) PastelPalette.AmberCardDark else PastelPalette.AmberCardLight,
                                    contentColor = if (isDark) PastelPalette.AmberAccentDark else PastelPalette.AmberAccent
                                ),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("quick_log_meal")
                            ) {
                                Icon(Icons.Rounded.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Log Meal")
                            }
                        }
                    }
                }
            }

            // Wellness Disclaimer
            item {
                WellnessDisclaimerBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActivityOverviewCard(
    todayState: TodayDashboardState,
    title: String,
    noDataText: String,
    sensorState: SensorUiState,
    onRequestStepPermission: () -> Unit
) {
    val stepsProgress = (todayState.steps.toFloat() / todayState.stepGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val activityProgress = (todayState.dailyExerciseMinutes.toFloat() / todayState.dailyExerciseGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val cardioProgress = (todayState.weeklyCardioMinutes.toFloat() / todayState.weeklyCardioGoal.coerceAtLeast(1)).coerceIn(0f, 1f)

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_overview_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${(stepsProgress * 100).roundToInt()}% daily goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActivityDualRingCanvas(
                    stepsProgress = stepsProgress,
                    activityProgress = activityProgress,
                    stepsText = if (todayState.hasStepData) String.format(Locale.US, "%,d", todayState.steps) else "0",
                    stepsLabel = "/ ${String.format(Locale.US, "%,d", todayState.stepGoal)}",
                    modifier = Modifier.size(134.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewStatRow(
                        label = "Daily Exercise",
                        value = "${todayState.dailyExerciseMinutes} / ${todayState.dailyExerciseGoal} min",
                        dotColor = PastelPalette.MintAccent
                    )
                    OverviewStatRow(
                        label = "Calories Burned",
                        value = if (todayState.hasCaloriesBurnedData) "Est. ${todayState.caloriesBurnedEst} kcal" else noDataText,
                        dotColor = PastelPalette.PeachAccent
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weekly Cardio",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${todayState.weeklyCardioMinutes}/${todayState.weeklyCardioGoal}m",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { cardioProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(MaterialTheme.shapes.small),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            if (sensorState.hasStepSensor && !sensorState.hasActivityPermission) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onRequestStepPermission)
                        .testTag("enable_step_sensor_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tap to allow Activity Recognition for live step sensor tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewStatRow(
    label: String,
    value: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = MaterialTheme.shapes.small,
                color = dotColor
            ) {}
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TodayHealthPastelCard(
    cardType: TodayCardType,
    todayState: TodayDashboardState,
    noDataText: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, accentColor) = when (cardType) {
        TodayCardType.STEPS -> if (isDark) PastelPalette.BlueCardDark to PastelPalette.BlueAccentDark else PastelPalette.BlueCardLight to PastelPalette.BlueAccent
        TodayCardType.READINESS -> if (isDark) PastelPalette.MintCardDark to PastelPalette.MintAccentDark else PastelPalette.MintCardLight to PastelPalette.MintAccent
        TodayCardType.SLEEP -> if (isDark) PastelPalette.PurpleCardDark to PastelPalette.PurpleAccentDark else PastelPalette.PurpleCardLight to PastelPalette.PurpleAccent
        TodayCardType.HEART_RATE -> if (isDark) PastelPalette.RoseCardDark to PastelPalette.RoseAccentDark else PastelPalette.RoseCardLight to PastelPalette.RoseAccent
        TodayCardType.CALORIES_BURNED -> if (isDark) PastelPalette.PeachCardDark to PastelPalette.PeachAccentDark else PastelPalette.PeachCardLight to PastelPalette.PeachAccent
        TodayCardType.DISTANCE -> if (isDark) PastelPalette.CyanCardDark to PastelPalette.CyanAccentDark else PastelPalette.CyanCardLight to PastelPalette.CyanAccent
    }

    val valueText: String
    val subtitleText: String
    val hasData: Boolean

    when (cardType) {
        TodayCardType.STEPS -> {
            hasData = todayState.hasStepData
            valueText = if (hasData) String.format(Locale.US, "%,d", todayState.steps) else noDataText
            subtitleText = if (hasData) "Goal ${String.format(Locale.US, "%,d", todayState.stepGoal)}" else "Sensor or manual"
        }
        TodayCardType.READINESS -> {
            val score = todayState.readinessScoreEst
            hasData = score != null
            valueText = if (score != null) "$score / 100" else noDataText
            subtitleText = if (score != null) "Est. from logs" else "Log sleep or HR"
        }
        TodayCardType.SLEEP -> {
            val mins = todayState.latestSleepMinutes
            hasData = mins != null
            valueText = if (mins != null) formatDurationMinutes(mins) else noDataText
            subtitleText = if (mins != null) "Latest sleep" else "Tap to log sleep"
        }
        TodayCardType.HEART_RATE -> {
            val bpm = todayState.latestHeartRateBpm
            hasData = bpm != null
            valueText = if (bpm != null) "$bpm bpm" else noDataText
            subtitleText = todayState.latestHeartRateSource ?: "No sensor reading"
        }
        TodayCardType.CALORIES_BURNED -> {
            hasData = todayState.hasCaloriesBurnedData
            valueText = if (hasData) "Est. ${todayState.caloriesBurnedEst}" else noDataText
            subtitleText = if (hasData) "kcal active est." else "Start a workout"
        }
        TodayCardType.DISTANCE -> {
            hasData = todayState.hasDistanceData
            val prefix = if (todayState.isDistanceEstimated) "Est. " else ""
            valueText = if (hasData) String.format(Locale.US, "%s%.2f km", prefix, todayState.distanceKm) else noDataText
            subtitleText = if (hasData) "Today total" else "Track walk or run"
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("today_card_${cardType.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricIconChip(icon = todayCardIcon(cardType), tint = accentColor)
                if (!hasData) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = noDataText,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = cardType.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (hasData) MaterialTheme.colorScheme.onSurface else accentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
