package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.IronLogDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

  @Test
  fun `bundled resources resolve cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("IronLog", context.getString(R.string.app_name))
    assertNotNull(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.space_grotesk))
    assertNotNull(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.inter))
    assertNotNull(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.jetbrains_mono))
    assertNotNull(androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_stat_ironlog))
  }

  @Test
  fun `main activity launches and navigates all tabs and settings without error`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_catalog").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_history").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_analytics").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_calc").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_train").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_button").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `starting and exiting empty workout returns to train tab content`() {
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("start_empty_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("start_empty_workout_button").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("discard_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("discard_workout_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_discard_workout_button").performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("start_empty_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("start_empty_workout_button").assertIsDisplayed()

    // Also test System Back + tab switching + Finish flow
    composeTestRule.onNodeWithTag("start_empty_workout_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("finish_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("resume_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("nav_catalog").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("nav_train").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("resume_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("resume_workout_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("finish_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("finish_workout_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodesWithTag("start_empty_workout_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("start_empty_workout_button").assertIsDisplayed()
  }

  @Test
  fun `offline repository seeds exercises and completes workout`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, IronLogDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val repo = WorkoutRepository(db, UserPreferencesRepository(context))
      repo.ensureSeeded()
      val exercises = repo.exercisesFlow.first()
      assertTrue(exercises.size >= 30)

      val routines = repo.routinesFlow.first()
      assertTrue(routines.isNotEmpty())

      val started = repo.startWorkoutFromRoutine(routines.first(), emptyList())
      assertNotNull(repo.activeWorkoutFlow.first())

      val withCompletedSet = started.copy(
        exercises = started.exercises.mapIndexed { exIdx, ex ->
          if (exIdx == 0) {
            ex.copy(sets = ex.sets.mapIndexed { sIdx, s -> if (sIdx == 0) s.copy(isCompleted = true) else s })
          } else ex
        }
      )
      val completed = repo.finishWorkout(withCompletedSet)
      assertEquals(1, repo.completedWorkoutsFlow.first().size)
      assertFalse(completed.isActive)
      assertNotNull(completed.finishedAtEpochMs)
    } finally {
      db.close()
    }
  }
}
