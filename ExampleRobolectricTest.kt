package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.WaterEntryEntity
import com.example.data.local.WeightEntryEntity
import com.example.ui.model.todayIsoDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var db: AppDatabase

    @Before
    fun setupDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches My Health`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My Health", appName)
    }

    @Test
    fun `room database persists weight and water entries`() = runTest {
        val dao = db.healthDao()
        val today = todayIsoDate()
        dao.insertWeight(WeightEntryEntity(date = today, timestampMillis = 1000L, weightKg = 68.5f))
        dao.insertWater(WaterEntryEntity(date = today, timestampMillis = 2000L, amountMl = 250))

        val weights = dao.observeAllWeights().first()
        val waters = dao.observeAllWater().first()

        assertEquals(1, weights.size)
        assertEquals(68.5f, weights.first().weightKg, 0.01f)
        assertEquals(1, waters.size)
        assertEquals(250, waters.first().amountMl)
    }

    @Test
    fun `today and health navigation tabs render and switch`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("activity_overview_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("today_log_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("today_start_workout_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("nav_tab_health").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("health_edit_metrics_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weight").assertIsDisplayed()
        assertTrue(true)
    }
}
