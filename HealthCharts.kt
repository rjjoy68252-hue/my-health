package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.model.ChartPoint
import com.example.ui.theme.PastelPalette
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActivityDualRingCanvas(
    stepsProgress: Float,
    activityProgress: Float,
    stepsText: String,
    stepsLabel: String,
    modifier: Modifier = Modifier,
    outerColor: Color = MaterialTheme.colorScheme.primary,
    innerColor: Color = PastelPalette.MintAccent
) {
    val animSteps by animateFloatAsState(
        targetValue = stepsProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "steps_ring"
    )
    val animActivity by animateFloatAsState(
        targetValue = activityProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "activity_ring"
    )
    val trackOuter = outerColor.copy(alpha = 0.14f)
    val trackInner = innerColor.copy(alpha = 0.16f)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeOuter = size.minDimension * 0.10f
            val strokeInner = size.minDimension * 0.085f
            val padOuter = strokeOuter / 2f
            val outerSize = Size(size.width - strokeOuter, size.height - strokeOuter)

            drawArc(
                color = trackOuter,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(padOuter, padOuter),
                size = outerSize,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
            )
            if (animSteps > 0.005f) {
                drawArc(
                    color = outerColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animSteps,
                    useCenter = false,
                    topLeft = Offset(padOuter, padOuter),
                    size = outerSize,
                    style = Stroke(width = strokeOuter, cap = StrokeCap.Round)
                )
            }

            val inset = strokeOuter + strokeInner * 0.9f
            val innerSize = Size(
                (size.width - inset * 2f).coerceAtLeast(10f),
                (size.height - inset * 2f).coerceAtLeast(10f)
            )
            drawArc(
                color = trackInner,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = innerSize,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round)
            )
            if (animActivity > 0.005f) {
                drawArc(
                    color = innerColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animActivity,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = innerSize,
                    style = Stroke(width = strokeInner, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stepsText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stepsLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniWeeklyBarChart(
    points: List<ChartPoint>,
    accentColor: Color,
    isLineStyle: Boolean = false,
    modifier: Modifier = Modifier
) {
    val nonNullValues = points.mapNotNull { it.value }
    val hasAny = nonNullValues.isNotEmpty()
    val maxVal = (nonNullValues.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minVal = if (isLineStyle && nonNullValues.size > 1) {
        (nonNullValues.minOrNull() ?: 0f) * 0.9f
    } else 0f
    val span = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            if (!hasAny) {
                // Clean dashed baseline indicating no recorded values
                drawLine(
                    color = accentColor.copy(alpha = 0.28f),
                    start = Offset(0f, size.height * 0.72f),
                    end = Offset(size.width, size.height * 0.72f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
                return@Canvas
            }

            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count

            if (isLineStyle) {
                val validPts = points.mapIndexedNotNull { idx, pt ->
                    pt.value?.let { v ->
                        val x = slotWidth * idx + slotWidth / 2f
                        val norm = ((v - minVal) / span).coerceIn(0.08f, 1f)
                        val y = size.height - (norm * (size.height * 0.8f)) - (size.height * 0.1f)
                        Offset(x, y)
                    }
                }
                if (validPts.size >= 2) {
                    val path = Path().apply {
                        moveTo(validPts.first().x, validPts.first().y)
                        for (i in 1 until validPts.size) {
                            lineTo(validPts[i].x, validPts[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                validPts.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = if (idx == validPts.lastIndex) accentColor else accentColor.copy(alpha = 0.65f),
                        radius = if (idx == validPts.lastIndex) 4.dp.toPx() else 2.8.dp.toPx(),
                        center = pt
                    )
                }
            } else {
                val barWidth = (slotWidth * 0.48f).coerceAtMost(12.dp.toPx())
                points.forEachIndexed { idx, pt ->
                    val x = slotWidth * idx + (slotWidth - barWidth) / 2f
                    val barColor = if (pt.value != null && pt.value > 0f) {
                        if (idx == points.lastIndex) accentColor else accentColor.copy(alpha = 0.55f)
                    } else {
                        accentColor.copy(alpha = 0.14f)
                    }
                    val norm = if (pt.value != null && pt.value > 0f) {
                        (pt.value / maxVal).coerceIn(0.15f, 1f)
                    } else {
                        0.12f
                    }
                    val barHeight = size.height * norm
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InteractiveMetricHistoryChart(
    points: List<ChartPoint>,
    accentColor: Color,
    unit: String,
    isLineChart: Boolean = false,
    valueFormatter: (Float) -> String = { v ->
        if (v % 1f == 0f) "${v.toInt()} $unit"
        else String.format(Locale.US, "%.1f %s", v, unit)
    },
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) {
        mutableIntStateOf(points.indexOfLast { it.value != null }.coerceAtLeast(-1))
    }

    val nonNull = points.mapNotNull { it.value }
    val maxVal = (nonNull.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minVal = if (isLineChart && nonNull.size > 1) {
        (nonNull.minOrNull() ?: 0f) * 0.92f
    } else 0f
    val span = (maxVal - minVal).coerceAtLeast(1f)

    Column(modifier = modifier) {
        val selectedPt = points.getOrNull(selectedIndex)
        Surface(
            color = accentColor.copy(alpha = 0.12f),
            shape = MaterialTheme.shapes.small
        ) {
            val summaryText = if (selectedPt != null && selectedPt.value != null) {
                val d = selectedPt.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                "$d: ${valueFormatter(selectedPt.value)}"
            } else if (selectedPt != null) {
                val d = selectedPt.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
                "$d: No data"
            } else {
                "Tap a point or bar to inspect"
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty()) {
                            val idx = ((offset.x / size.width) * points.size)
                                .toInt()
                                .coerceIn(0, points.lastIndex)
                            selectedIndex = idx
                        }
                    }
                }
        ) {
            val count = points.size.coerceAtLeast(1)
            val slotWidth = size.width / count

            // Horizontal reference lines
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = size.height * (1f - frac * 0.85f)
                drawLine(
                    color = accentColor.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (nonNull.isEmpty()) {
                drawLine(
                    color = accentColor.copy(alpha = 0.35f),
                    start = Offset(0f, size.height * 0.7f),
                    end = Offset(size.width, size.height * 0.7f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                )
                return@Canvas
            }

            if (isLineChart) {
                val mapped = points.mapIndexedNotNull { idx, pt ->
                    pt.value?.let { v ->
                        val x = slotWidth * idx + slotWidth / 2f
                        val norm = ((v - minVal) / span).coerceIn(0.05f, 1f)
                        val y = size.height - (norm * size.height * 0.82f) - size.height * 0.08f
                        idx to Offset(x, y)
                    }
                }
                if (mapped.size >= 2) {
                    val path = Path().apply {
                        moveTo(mapped.first().second.x, mapped.first().second.y)
                        for (i in 1 until mapped.size) {
                            lineTo(mapped[i].second.x, mapped[i].second.y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                mapped.forEach { (idx, pt) ->
                    val isSel = idx == selectedIndex
                    if (isSel) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.22f),
                            radius = 11.dp.toPx(),
                            center = pt
                        )
                    }
                    drawCircle(
                        color = accentColor,
                        radius = if (isSel) 5.5.dp.toPx() else 3.8.dp.toPx(),
                        center = pt
                    )
                }
            } else {
                val barW = (slotWidth * 0.56f).coerceIn(4.dp.toPx(), 24.dp.toPx())
                points.forEachIndexed { idx, pt ->
                    val x = slotWidth * idx + (slotWidth - barW) / 2f
                    val isSel = idx == selectedIndex
                    val v = pt.value
                    if (v != null && v > 0f) {
                        val norm = (v / maxVal).coerceIn(0.08f, 1f)
                        val barH = size.height * 0.85f * norm
                        drawRoundRect(
                            color = if (isSel) accentColor else accentColor.copy(alpha = 0.52f),
                            topLeft = Offset(x, size.height - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    } else {
                        val baseH = 6.dp.toPx()
                        drawRoundRect(
                            color = if (isSel) accentColor.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.12f),
                            topLeft = Offset(x, size.height - baseH),
                            size = Size(barW, baseH),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (points.size > 10) 5 else 1
            points.forEachIndexed { idx, pt ->
                if (idx % step == 0 || idx == points.lastIndex) {
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (idx == selectedIndex) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
