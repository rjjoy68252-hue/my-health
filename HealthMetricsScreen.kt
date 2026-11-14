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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.UserProfileAndSettings
import com.example.ui.components.MetricIconChip
import com.example.ui.components.MiniWeeklyBarChart
import com.example.ui.components.ProfileAvatarButton
import com.example.ui.components.WellnessDisclaimerBanner
import com.example.ui.components.metricIcon
import com.example.ui.model.MetricCardSummary
import com.example.ui.model.MetricType
import com.example.ui.model.WellnessStrings
import com.example.ui.theme.LocalIsDarkTheme

@Composable
fun HealthMetricsScreen(
    metricCards: List<MetricCardSummary>,
    settings: UserProfileAndSettings,
    onEditMetricsClick: () -> Unit,
    onCardClick: (MetricType) -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val lang = settings.language
    val noDataText = WellnessStrings.noData(lang)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = WellnessStrings.tabHealth(lang),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${metricCards.size} tracked metrics • Tap any card for trends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onEditMetricsClick,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("health_edit_metrics_button"),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(WellnessStrings.editAction(lang))
                        }
                        ProfileAvatarButton(onClick = onOpenProfile)
                    }
                }
            }

            val rows = metricCards.chunked(columns)
            items(rows.size) { rowIndex ->
                val rowItems = rows[rowIndex]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { cardSummary ->
                        HealthMetricGridCard(
                            summary = cardSummary,
                            title = WellnessStrings.metricTitle(cardSummary.type, lang),
                            noDataText = noDataText,
                            isDark = isDark,
                            onClick = { onCardClick(cardSummary.type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                WellnessDisclaimerBanner()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun HealthMetricGridCard(
    summary: MetricCardSummary,
    title: String,
    noDataText: String,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (pastelBg, accent) = summary.type.cardColors(isDark)
    val isLine = summary.type == MetricType.WEIGHT || summary.type == MetricType.HEART_RATE

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, pastelBg.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag("health_metric_card_${summary.type.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricIconChip(icon = metricIcon(summary.type), tint = accent)
                if (!summary.hasData) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = pastelBg
                    ) {
                        Text(
                            text = noDataText,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (summary.type.isEstimated || summary.isEstimatedLabel) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = pastelBg
                    ) {
                        Text(
                            text = "Est.",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = summary.currentValueDisplay,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.hasData) MaterialTheme.colorScheme.onSurface else accent.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            MiniWeeklyBarChart(
                points = summary.weeklyPoints,
                accentColor = accent,
                isLineStyle = isLine
            )

            Text(
                text = summary.previousDaysSummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
