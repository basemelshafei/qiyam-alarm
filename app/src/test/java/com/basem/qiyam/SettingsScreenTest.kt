package com.basem.qiyam

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.basem.qiyam.ui.Motion

/** Taps the real settings on the real screen and checks they change, on a simulated phone. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SettingsScreenTest {

    init {
        // Must be off before the screen opens: a screen that never stops moving never settles.
        Motion.live = false
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() = rule.waitForIdle()

    @Test
    fun wakeOffsetChipsChangeAndShowSelection() {
        rule.onNodeWithText("At start").performScrollTo().assertIsSelected()

        rule.onNodeWithText("+20m").performScrollTo().performClick()
        settle()

        rule.onNodeWithText("+20m").assertIsSelected()
        rule.onNodeWithText("At start").assertIsNotSelected()
        assertEquals(20, Prefs(rule.activity).offsetMinutes)
        rule.onNodeWithText("20 min into the last third", substring = true).assertExists()
    }

    @Test
    fun headsUpAndSnoozeChipsChange() {
        rule.onNodeWithText("30m").performScrollTo().performClick()
        settle()
        rule.onNodeWithText("30m").assertIsSelected()
        assertEquals(30, Prefs(rule.activity).preAlarmMinutes)

        // "15m" exists in both rows; the second one is the snooze length.
        rule.onAllNodesWithText("15m")[1].performScrollTo().performClick()
        settle()
        rule.onAllNodesWithText("15m")[1].assertIsSelected()
        rule.onAllNodesWithText("15m")[0].assertIsNotSelected()
        assertEquals(15, Prefs(rule.activity).snoozeMinutes)
    }

    @Test
    fun calculationMethodChanges() {
        rule.onNodeWithText("Muslim World League").performScrollTo().performClick()
        settle()
        rule.onNodeWithText("Umm al-Qura (Makkah)").performClick()
        settle()
        assertEquals(Method.UMM_AL_QURA, Prefs(rule.activity).method)
        rule.onNodeWithText("Umm al-Qura (Makkah)").assertExists()
    }
}
