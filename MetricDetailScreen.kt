package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.InteractiveMetricHistoryChart
import com.example.ui.components.MetricIconChip
import com.example.ui.components.WellnessDisclaimerBanner
import com.example.ui.components.metricIcon
import com.example.ui.model.ChartPoint
import com.example.ui.model.DateRangeOption
import com.example.ui.model.MetricType
import com.example.ui.model.WellnessStrings
import com.example.ui.model.formatDurationMinutes
import com.example.ui.model.formatStopwatch
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.viewmodel.HealthRawSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DetailRecordItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val valueText: String,
    val onDelete: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScreen(
    metricType: MetricType,
    snapshot: HealthRawSnapshot,
    getChartPoints: (MetricType, DateRangeOption) -> List<ChartPoint>,
    onLogMetricClick: (MetricType) -> Unit,
    onDeleteWeight: (Long) -> Unit,
    onDeleteSleep: (Long) -> Unit,
    onDeleteHeartRate: (Long) -> Unit,
    onDeleteWater: (Long) -> Unit,
    onDeleteNutrition: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isDark = LocalIsDarkTheme.current
    val lang = snapshot.settings.language
    val (pastelBg, accent) = metricType.cardColors(isDark)
    var selectedRange by remember { mutableStateOf(DateRangeOption.WEEKLY) }
    val chartPoints = remember(snapshot, metricType, selectedRange) {
        getChartPoints(metricType, selectedRange)
    }

    val isLine = metricType == MetricType.WEIGHT || metricType == MetricType.HEART_RATE
    val nonNull = chartPoints.mapNotNull { it.value }
    val avgValue = if (nonNull.isNotEmpty()) nonNull.average().toFloat() else null
    val maxValue = nonNull.maxOrNull()

    val entries = remember(snapshot, metricType) {
        buildDetailEntries(
            metricType = metricType,
            snapshot = snapshot,
            onDeleteWeight = onDeleteWeight,
            onDeleteSleep = onDeleteSleep,
            onDeleteHeartRate = onDeleteHeartRate,
            onDeleteWater = onDeleteWater,
            onDeleteNutrition = onDeleteNutrition,
            onDeleteWorkout = onDeleteWorkout
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(WellnessStrings.metricTitle(metricType, lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("metric_detail_back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { onLogMetricClick(metricType) },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("metric_detail_log_button"),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Log")
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
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Range Selector Chips (Daily / Weekly / Monthly)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DateRangeOption.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("range_chip_${range.name.lowercase()}")
                        )
                    }
                }
            }

            // Interactive Chart Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, pastelBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricIconChip(icon = metricIcon(metricType), tint = accent)
                            Column {
                                Text(
                                    text = "${selectedRange.label} Trend",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (metricType.isEstimated) {
                                    Text(
                                        text = "Values marked Est. are calculated estimates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        InteractiveMetricHistoryChart(
                            points = chartPoints,
                            accentColor = accent,
                            unit = metricType.unit,
                            isLineChart = isLine,
                            valueFormatter = { v -> formatMetricFloat(metricType, v) }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Period Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = avgValue?.let { formatMetricFloat(metricType, it) } ?: WellnessStrings.noData(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Period Peak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = maxValue?.let { formatMetricFloat(metricType, it) } ?: WellnessStrings.noData(lang),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Recorded Entries",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (entries.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No entries yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap Log above to add your first ${metricType.titleEn.lowercase()} record.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(entries, key = { "${metricType.id}_${it.id}_${it.subtitle}" }) { item ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.valueText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                if (item.onDelete != null) {
                                    IconButton(onClick = item.onDelete) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete entry",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                WellnessDisclaimerBanner()
            }
        }
    }
}

private fun formatMetricFloat(type: MetricType, v: Float): String = when (type) {
    MetricType.WEIGHT -> String.format(Locale.US, "%.1f kg", v)
    MetricType.SLEEP_DURATION -> formatDurationMinutes((v * 60).roundToInt())
    MetricType.WALKING_DISTANCE -> String.format(Locale.US, "Est. %.2f km", v)
    MetricType.EXERCISE_DAYS -> "${v.roundToInt()} min"
    MetricType.ENERGY_BURNED -> "Est. ${v.roundToInt()} kcal"
    else -> "${v.roundToInt()} ${type.unit}"
}

private fun buildDetailEntries(
    metricType: MetricType,
    snapshot: HealthRawSnapshot,
    onDeleteWeight: (Long) -> Unit,
    onDeleteSleep: (Long) -> Unit,
    onDeleteHeartRate: (Long) -> Unit,
    onDeleteWater: (Long) -> Unit,
    onDeleteNutrition: (Long) -> Unit,
    onDeleteWorkout: (Long) -> Unit
): List<DetailRecordItem> {
    val timeFmt = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return when (metricType) {
        MetricType.WEIGHT -> snapshot.weights.map { w ->
            DetailRecordItem(
                id = w.id,
                title = "Weight (${w.source})",
                subtitle = timeFmt.format(Date(w.timestampMillis)),
                valueText = String.format(Locale.US, "%.1f kg", w.weightKg),
                onDelete = { onDeleteWeight(w.id) }
            )
        }
        MetricType.HEART_RATE -> snapshot.heartRates.map { hr ->
            DetailRecordItem(
                id = hr.id,
                title = "Heart Rate (${hr.source})",
                subtitle = timeFmt.format(Date(hr.timestampMillis)),
                valueText = "${hr.bpm} bpm",
                onDelete = { onDeleteHeartRate(hr.id) }
            )
        }
        MetricType.SLEEP_DURATION -> snapshot.sleeps.map { s ->
            DetailRecordItem(
                id = s.id,
                title = "${s.qualityLabel} (${s.source})",
                subtitle = s.date,
                valueText = formatDurationMinutes(s.durationMinutes),
                onDelete = { onDeleteSleep(s.id) }
            )
        }
        MetricType.WATER_INTAKE -> snapshot.waters.map { w ->
            DetailRecordItem(
                id = w.id,
                title = "Water (${w.source})",
                subtitle = timeFmt.format(Date(w.timestampMillis)),
                valueText = "${w.amountMl} ml",
                onDelete = { onDeleteWater(w.id) }
            )
        }
        MetricType.CALORIES_INTAKE, MetricType.CARBS, MetricType.FAT, MetricType.PROTEIN -> {
            snapshot.nutritions.map { n ->
                val value = when (metricType) {
                    MetricType.CALORIES_INTAKE -> "${n.calories} kcal"
                    MetricType.CARBS -> "${n.carbsGrams.roundToInt()} g carbs"
                    MetricType.FAT -> "${n.fatGrams.roundToInt()} g fat"
                    else -> "${n.proteinGrams.roundToInt()} g protein"
                }
                DetailRecordItem(
                    id = n.id,
                    title = "${n.mealName} (${n.mealType})",
                    subtitle = "${timeFmt.format(Date(n.timestampMillis))} • ${n.calories} kcal • C:${n.carbsGrams.roundToInt()}g F:${n.fatGrams.roundToInt()}g P:${n.proteinGrams.roundToInt()}g",
                    valueText = value,
                    onDelete = { onDeleteNutrition(n.id) }
                )
            }
        }
        MetricType.EXERCISE_DAYS -> snapshot.workouts.map { w ->
            DetailRecordItem(
                id = w.id,
                title = w.title,
                subtitle = timeFmt.format(Date(w.startTimeMillis)),
                valueText = "${formatStopwatch(w.durationSeconds)} • Est. ${w.caloriesBurnedEst} kcal",
                onDelete = { onDeleteWorkout(w.id) }
            )
        }
        MetricType.STEPS, MetricType.ENERGY_BURNED, MetricType.WALKING_DISTANCE -> {
            snapshot.activities.filter {
                it.steps > 0 || it.caloriesBurnedEst > 0 || it.distanceKm > 0f
            }.mapIndexed { idx, act ->
                val valStr = when (metricType) {
                    MetricType.STEPS -> String.format(Locale.US, "%,d steps", act.steps)
                    MetricType.ENERGY_BURNED -> "Est. ${act.caloriesBurnedEst} kcal"
                    else -> String.format(Locale.US, "Est. %.2f km", act.distanceKm)
                }
                DetailRecordItem(
                    id = idx.toLong(),
                    title = "Daily Activity (${act.source})",
                    subtitle = act.date,
                    valueText = valStr,
                    onDelete = null
                )
            }
        }
    }
}
