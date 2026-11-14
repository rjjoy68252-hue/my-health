package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.model.MetricType
import com.example.ui.model.TodayCardType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "my_health_prefs")

data class UserProfileAndSettings(
    val name: String = "",
    val age: Int? = null,
    val heightCm: Float? = null,
    val gender: String = "",
    val dailyStepGoal: Int = 8000,
    val dailyWaterGoalMl: Int = 2000,
    val dailyExerciseGoalMin: Int = 30,
    val weeklyCardioGoalMin: Int = 150,
    val dailyCalorieIntakeGoal: Int = 2000,
    val workoutRemindersEnabled: Boolean = false,
    val hydrationRemindersEnabled: Boolean = false,
    val darkMode: String = "System", // "System", "Light", "Dark"
    val language: String = "English", // "English", "Spanish", "French", "German"
    val healthConnectSyncEnabled: Boolean = false,
    val sensorStepTrackingEnabled: Boolean = true,
    val visibleMetricIds: Set<String> = MetricType.entries.map { it.id }.toSet(),
    val visibleTodayCardIds: Set<String> = TodayCardType.entries.map { it.id }.toSet(),
    val lastSensorCounterValue: Float = -1f,
    val lastSensorDate: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("profile_name")
        val AGE = intPreferencesKey("profile_age")
        val HEIGHT_CM = floatPreferencesKey("profile_height_cm")
        val GENDER = stringPreferencesKey("profile_gender")
        val DAILY_STEP_GOAL = intPreferencesKey("goal_daily_steps")
        val DAILY_WATER_GOAL = intPreferencesKey("goal_daily_water_ml")
        val DAILY_EXERCISE_GOAL = intPreferencesKey("goal_daily_exercise_min")
        val WEEKLY_CARDIO_GOAL = intPreferencesKey("goal_weekly_cardio_min")
        val DAILY_CALORIE_GOAL = intPreferencesKey("goal_daily_calories")
        val WORKOUT_REMINDERS = booleanPreferencesKey("notif_workout_reminders")
        val HYDRATION_REMINDERS = booleanPreferencesKey("notif_hydration_reminders")
        val DARK_MODE = stringPreferencesKey("ui_dark_mode")
        val LANGUAGE = stringPreferencesKey("ui_language")
        val HEALTH_CONNECT_SYNC = booleanPreferencesKey("hc_sync_enabled")
        val SENSOR_STEP_TRACKING = booleanPreferencesKey("sensor_step_tracking")
        val VISIBLE_METRICS = stringSetPreferencesKey("visible_metric_ids")
        val VISIBLE_TODAY_CARDS = stringSetPreferencesKey("visible_today_card_ids")
        val LAST_SENSOR_VALUE = floatPreferencesKey("last_sensor_counter_value")
        val LAST_SENSOR_DATE = stringPreferencesKey("last_sensor_date")
    }

    val settingsFlow: Flow<UserProfileAndSettings> = context.dataStore.data.map { prefs ->
        val ageVal = prefs[Keys.AGE]
        val heightVal = prefs[Keys.HEIGHT_CM]
        UserProfileAndSettings(
            name = prefs[Keys.NAME] ?: "",
            age = if (ageVal != null && ageVal > 0) ageVal else null,
            heightCm = if (heightVal != null && heightVal > 0f) heightVal else null,
            gender = prefs[Keys.GENDER] ?: "",
            dailyStepGoal = prefs[Keys.DAILY_STEP_GOAL] ?: 8000,
            dailyWaterGoalMl = prefs[Keys.DAILY_WATER_GOAL] ?: 2000,
            dailyExerciseGoalMin = prefs[Keys.DAILY_EXERCISE_GOAL] ?: 30,
            weeklyCardioGoalMin = prefs[Keys.WEEKLY_CARDIO_GOAL] ?: 150,
            dailyCalorieIntakeGoal = prefs[Keys.DAILY_CALORIE_GOAL] ?: 2000,
            workoutRemindersEnabled = prefs[Keys.WORKOUT_REMINDERS] ?: false,
            hydrationRemindersEnabled = prefs[Keys.HYDRATION_REMINDERS] ?: false,
            darkMode = prefs[Keys.DARK_MODE] ?: "System",
            language = prefs[Keys.LANGUAGE] ?: "English",
            healthConnectSyncEnabled = prefs[Keys.HEALTH_CONNECT_SYNC] ?: false,
            sensorStepTrackingEnabled = prefs[Keys.SENSOR_STEP_TRACKING] ?: true,
            visibleMetricIds = prefs[Keys.VISIBLE_METRICS] ?: MetricType.entries.map { it.id }.toSet(),
            visibleTodayCardIds = prefs[Keys.VISIBLE_TODAY_CARDS] ?: TodayCardType.entries.map { it.id }.toSet(),
            lastSensorCounterValue = prefs[Keys.LAST_SENSOR_VALUE] ?: -1f,
            lastSensorDate = prefs[Keys.LAST_SENSOR_DATE] ?: ""
        )
    }

    suspend fun updateProfile(
        name: String,
        age: Int?,
        heightCm: Float?,
        gender: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = name.trim()
            if (age != null && age > 0) prefs[Keys.AGE] = age else prefs.remove(Keys.AGE)
            if (heightCm != null && heightCm > 0f) prefs[Keys.HEIGHT_CM] = heightCm else prefs.remove(Keys.HEIGHT_CM)
            prefs[Keys.GENDER] = gender
        }
    }

    suspend fun updateGoals(
        dailySteps: Int,
        dailyWaterMl: Int,
        dailyExerciseMin: Int,
        weeklyCardioMin: Int,
        dailyCalorieGoal: Int
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAILY_STEP_GOAL] = dailySteps.coerceIn(500, 100000)
            prefs[Keys.DAILY_WATER_GOAL] = dailyWaterMl.coerceIn(250, 10000)
            prefs[Keys.DAILY_EXERCISE_GOAL] = dailyExerciseMin.coerceIn(5, 600)
            prefs[Keys.WEEKLY_CARDIO_GOAL] = weeklyCardioMin.coerceIn(15, 2000)
            prefs[Keys.DAILY_CALORIE_GOAL] = dailyCalorieGoal.coerceIn(500, 10000)
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_MODE] = mode }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language }
    }

    suspend fun setWorkoutReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WORKOUT_REMINDERS] = enabled }
    }

    suspend fun setHydrationReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HYDRATION_REMINDERS] = enabled }
    }

    suspend fun setHealthConnectSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HEALTH_CONNECT_SYNC] = enabled }
    }

    suspend fun setSensorStepTracking(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SENSOR_STEP_TRACKING] = enabled }
    }

    suspend fun setVisibleMetricIds(ids: Set<String>) {
        context.dataStore.edit { it[Keys.VISIBLE_METRICS] = ids }
    }

    suspend fun setVisibleTodayCardIds(ids: Set<String>) {
        context.dataStore.edit { it[Keys.VISIBLE_TODAY_CARDS] = ids }
    }

    suspend fun updateLastSensorCounter(value: Float, date: String) {
        context.dataStore.edit {
            it[Keys.LAST_SENSOR_VALUE] = value
            it[Keys.LAST_SENSOR_DATE] = date
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
