package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EggAlt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.model.MetricType
import com.example.ui.model.TodayCardType
import com.example.ui.model.WorkoutCategory

fun metricIcon(type: MetricType): ImageVector = when (type) {
    MetricType.WEIGHT -> Icons.Rounded.MonitorWeight
    MetricType.ENERGY_BURNED -> Icons.Rounded.LocalFireDepartment
    MetricType.CALORIES_INTAKE -> Icons.Rounded.Restaurant
    MetricType.CARBS -> Icons.Rounded.Grain
    MetricType.FAT -> Icons.Rounded.WaterDrop
    MetricType.PROTEIN -> Icons.Rounded.EggAlt
    MetricType.STEPS -> Icons.AutoMirrored.Rounded.DirectionsWalk
    MetricType.EXERCISE_DAYS -> Icons.Rounded.CalendarMonth
    MetricType.HEART_RATE -> Icons.Rounded.Favorite
    MetricType.SLEEP_DURATION -> Icons.Rounded.Bedtime
    MetricType.WALKING_DISTANCE -> Icons.Rounded.Route
    MetricType.WATER_INTAKE -> Icons.Rounded.WaterDrop
}

fun todayCardIcon(type: TodayCardType): ImageVector = when (type) {
    TodayCardType.STEPS -> Icons.AutoMirrored.Rounded.DirectionsWalk
    TodayCardType.READINESS -> Icons.Rounded.AutoAwesome
    TodayCardType.SLEEP -> Icons.Rounded.Bedtime
    TodayCardType.HEART_RATE -> Icons.Rounded.Favorite
    TodayCardType.CALORIES_BURNED -> Icons.Rounded.LocalFireDepartment
    TodayCardType.DISTANCE -> Icons.Rounded.Route
}

fun workoutCategoryIcon(category: WorkoutCategory): ImageVector = when (category) {
    WorkoutCategory.WALKING -> Icons.AutoMirrored.Rounded.DirectionsWalk
    WorkoutCategory.RUNNING -> Icons.AutoMirrored.Rounded.DirectionsRun
    WorkoutCategory.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
    WorkoutCategory.GYM -> Icons.Rounded.FitnessCenter
    WorkoutCategory.STRENGTH_TRAINING -> Icons.Rounded.FitnessCenter
    WorkoutCategory.FOOTBALL -> Icons.Rounded.SportsSoccer
    WorkoutCategory.OTHER -> Icons.Rounded.AutoAwesome
}

@Composable
fun MetricIconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileAvatarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("profile_avatar_button"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_profile_avatar),
            contentDescription = "Open Profile and Settings",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
        )
    }
}

@Composable
fun WellnessDisclaimerBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.wellness_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
