package com.example.ui.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PastelPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class MetricType(
    val id: String,
    val titleEn: String,
    val unit: String,
    val isEstimated: Boolean = false
) {
    WEIGHT("weight", "Weight", "kg"),
    ENERGY_BURNED("energy_burned", "Energy Burned", "kcal", isEstimated = true),
    CALORIES_INTAKE("calories_intake", "Calories Intake", "kcal"),
    CARBS("carbs", "Carbs", "g"),
    FAT("fat", "Fat", "g"),
    PROTEIN("protein", "Protein", "g"),
    STEPS("steps", "Steps", "steps"),
    EXERCISE_DAYS("exercise_days", "Exercise Days", "days"),
    HEART_RATE("heart_rate", "Heart Rate", "bpm"),
    SLEEP_DURATION("sleep_duration", "Sleep Duration", "hrs"),
    WALKING_DISTANCE("walking_distance", "Walking Distance", "km", isEstimated = true),
    WATER_INTAKE("water_intake", "Water Intake", "ml");

    fun cardColors(isDark: Boolean): Pair<Color, Color> = when (this) {
        WEIGHT -> if (isDark) PastelPalette.PurpleCardDark to PastelPalette.PurpleAccentDark else PastelPalette.PurpleCardLight to PastelPalette.PurpleAccent
        ENERGY_BURNED -> if (isDark) PastelPalette.PeachCardDark to PastelPalette.PeachAccentDark else PastelPalette.PeachCardLight to PastelPalette.PeachAccent
        CALORIES_INTAKE -> if (isDark) PastelPalette.AmberCardDark to PastelPalette.AmberAccentDark else PastelPalette.AmberCardLight to PastelPalette.AmberAccent
        CARBS -> if (isDark) PastelPalette.AmberCardDark to PastelPalette.AmberAccentDark else PastelPalette.AmberCardLight to PastelPalette.AmberAccent
        FAT -> if (isDark) PastelPalette.PeachCardDark to PastelPalette.PeachAccentDark else PastelPalette.PeachCardLight to PastelPalette.PeachAccent
        PROTEIN -> if (isDark) PastelPalette.MintCardDark to PastelPalette.MintAccentDark else PastelPalette.MintCardLight to PastelPalette.MintAccent
        STEPS -> if (isDark) PastelPalette.BlueCardDark to PastelPalette.BlueAccentDark else PastelPalette.BlueCardLight to PastelPalette.BlueAccent
        EXERCISE_DAYS -> if (isDark) PastelPalette.MintCardDark to PastelPalette.MintAccentDark else PastelPalette.MintCardLight to PastelPalette.MintAccent
        HEART_RATE -> if (isDark) PastelPalette.RoseCardDark to PastelPalette.RoseAccentDark else PastelPalette.RoseCardLight to PastelPalette.RoseAccent
        SLEEP_DURATION -> if (isDark) PastelPalette.PurpleCardDark to PastelPalette.PurpleAccentDark else PastelPalette.PurpleCardLight to PastelPalette.PurpleAccent
        WALKING_DISTANCE -> if (isDark) PastelPalette.CyanCardDark to PastelPalette.CyanAccentDark else PastelPalette.CyanCardLight to PastelPalette.CyanAccent
        WATER_INTAKE -> if (isDark) PastelPalette.CyanCardDark to PastelPalette.CyanAccentDark else PastelPalette.CyanCardLight to PastelPalette.CyanAccent
    }
}

enum class TodayCardType(val id: String, val title: String) {
    STEPS("today_steps", "Steps"),
    READINESS("today_readiness", "Readiness"),
    SLEEP("today_sleep", "Sleep"),
    HEART_RATE("today_heart_rate", "Heart Rate"),
    CALORIES_BURNED("today_calories", "Calories Burned"),
    DISTANCE("today_distance", "Distance Traveled")
}

enum class WorkoutCategory(
    val id: String,
    val displayName: String,
    val metValue: Double,
    val tracksDistance: Boolean
) {
    WALKING("walking", "Walking", 3.5, true),
    RUNNING("running", "Running", 8.5, true),
    CYCLING("cycling", "Cycling", 6.8, true),
    GYM("gym", "Gym", 5.0, false),
    STRENGTH_TRAINING("strength", "Strength Training", 5.5, false),
    FOOTBALL("football", "Football", 7.5, true),
    OTHER("other", "Other Exercises", 4.5, true)
}

enum class MealType(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack")
}

enum class DateRangeOption(val label: String, val days: Int) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30)
}

data class ChartPoint(
    val date: LocalDate,
    val label: String,
    val value: Float?
)

data class MetricCardSummary(
    val type: MetricType,
    val currentValueDisplay: String,
    val hasData: Boolean,
    val previousDaysSummary: String,
    val weeklyPoints: List<ChartPoint>,
    val isEstimatedLabel: Boolean = false
)

data class TodayDashboardState(
    val dateLabel: String = "",
    val steps: Int = 0,
    val hasStepData: Boolean = false,
    val stepGoal: Int = 8000,
    val weeklyCardioMinutes: Int = 0,
    val weeklyCardioGoal: Int = 150,
    val dailyExerciseMinutes: Int = 0,
    val dailyExerciseGoal: Int = 30,
    val caloriesBurnedEst: Int = 0,
    val hasCaloriesBurnedData: Boolean = false,
    val distanceKm: Float = 0f,
    val hasDistanceData: Boolean = false,
    val isDistanceEstimated: Boolean = true,
    val readinessScoreEst: Int? = null,
    val latestSleepMinutes: Int? = null,
    val latestHeartRateBpm: Int? = null,
    val latestHeartRateSource: String? = null,
    val waterIntakeMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val caloriesIntake: Int = 0,
    val calorieIntakeGoal: Int = 2000
)

data class ActiveWorkoutState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val category: WorkoutCategory = WorkoutCategory.WALKING,
    val elapsedSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val isDistanceGpsBacked: Boolean = false,
    val stepsTaken: Int = 0,
    val estimatedCalories: Int = 0,
    val startTimeMillis: Long = 0L
)

data class HealthConnectStatus(
    val isAvailable: Boolean = false,
    val needsProviderUpdate: Boolean = false,
    val isSupportedDevice: Boolean = true,
    val hasPermissions: Boolean = false,
    val lastSyncMessage: String? = null
)

object WellnessStrings {
    fun tabToday(lang: String): String = when (lang) {
        "Spanish" -> "Hoy"
        "French" -> "Aujourd'hui"
        "German" -> "Heute"
        else -> "Today"
    }

    fun tabHealth(lang: String): String = when (lang) {
        "Spanish" -> "Salud"
        "French" -> "Santé"
        "German" -> "Gesundheit"
        else -> "Health"
    }

    fun noData(lang: String): String = when (lang) {
        "Spanish" -> "Sin datos"
        "French" -> "Aucune donnée"
        "German" -> "Keine Daten"
        else -> "No data"
    }

    fun activityOverview(lang: String): String = when (lang) {
        "Spanish" -> "Resumen de actividad"
        "French" -> "Aperçu d'activité"
        "German" -> "Aktivitätsübersicht"
        else -> "Activity Overview"
    }

    fun startWorkout(lang: String): String = when (lang) {
        "Spanish" -> "Entrenar"
        "French" -> "Exercice"
        "German" -> "Training"
        else -> "Start Workout"
    }

    fun logAction(lang: String): String = when (lang) {
        "Spanish" -> "Registrar"
        "French" -> "Ajouter"
        "German" -> "Erfassen"
        else -> "Log"
    }

    fun editAction(lang: String): String = when (lang) {
        "Spanish" -> "Editar"
        "French" -> "Modifier"
        "German" -> "Bearbeiten"
        else -> "Edit"
    }

    fun metricTitle(type: MetricType, lang: String): String = when (lang) {
        "Spanish" -> when (type) {
            MetricType.WEIGHT -> "Peso"
            MetricType.ENERGY_BURNED -> "Energía quemada"
            MetricType.CALORIES_INTAKE -> "Calorías consumidas"
            MetricType.CARBS -> "Carbohidratos"
            MetricType.FAT -> "Grasas"
            MetricType.PROTEIN -> "Proteínas"
            MetricType.STEPS -> "Pasos"
            MetricType.EXERCISE_DAYS -> "Días de ejercicio"
            MetricType.HEART_RATE -> "Ritmo cardíaco"
            MetricType.SLEEP_DURATION -> "Duración del sueño"
            MetricType.WALKING_DISTANCE -> "Distancia"
            MetricType.WATER_INTAKE -> "Consumo de agua"
        }
        "French" -> when (type) {
            MetricType.WEIGHT -> "Poids"
            MetricType.ENERGY_BURNED -> "Énergie brûlée"
            MetricType.CALORIES_INTAKE -> "Calories consommées"
            MetricType.CARBS -> "Glucides"
            MetricType.FAT -> "Lipides"
            MetricType.PROTEIN -> "Protéines"
            MetricType.STEPS -> "Pas"
            MetricType.EXERCISE_DAYS -> "Jours d'exercice"
            MetricType.HEART_RATE -> "Fréquence cardiaque"
            MetricType.SLEEP_DURATION -> "Sommeil"
            MetricType.WALKING_DISTANCE -> "Distance"
            MetricType.WATER_INTAKE -> "Hydratation"
        }
        "German" -> when (type) {
            MetricType.WEIGHT -> "Gewicht"
            MetricType.ENERGY_BURNED -> "Verbrannte Energie"
            MetricType.CALORIES_INTAKE -> "Kalorienaufnahme"
            MetricType.CARBS -> "Kohlenhydrate"
            MetricType.FAT -> "Fett"
            MetricType.PROTEIN -> "Protein"
            MetricType.STEPS -> "Schritte"
            MetricType.EXERCISE_DAYS -> "Trainingstage"
            MetricType.HEART_RATE -> "Herzfrequenz"
            MetricType.SLEEP_DURATION -> "Schlafdauer"
            MetricType.WALKING_DISTANCE -> "Gehstrecke"
            MetricType.WATER_INTAKE -> "Wasseraufnahme"
        }
        else -> type.titleEn
    }
}

fun formatDurationMinutes(totalMinutes: Int): String {
    val hrs = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
        hrs > 0 -> "${hrs}h"
        else -> "${mins}m"
    }
}

fun formatStopwatch(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

fun todayIsoDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
