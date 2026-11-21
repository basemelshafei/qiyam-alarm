package com.basem.qiyam

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.basem.qiyam.ui.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Ticks things off on the real screens and checks they're recorded and shown. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TrackerScreenTest {

    init {
        Motion.live = false
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val today get() = LocalDate.now(Prefs(rule.activity).zone)
    private val log get() = DayLog(rule.activity)

    private fun settle() = rule.waitForIdle()

    @Test
    fun tickingAPrayerOnTodayRecordsItAndRaisesTheScore() {
        val before = Plan.scoreFor(Prefs(rule.activity), log, today, Prefs(rule.activity).zone)
        rule.onNode(hasText("Dhuhr") and isToggleable()).performScrollTo().performClick()
        settle()
        assertEquals(PrayerStatus.ON_TIME, log.prayer(today, Prayer.DHUHR))
        rule.onNode(hasText("Dhuhr") and isToggleable()).assertIsOn()
        val after = Plan.scoreFor(Prefs(rule.activity), log, today, Prefs(rule.activity).zone)
        assertTrue("score $before -> $after", after > before)
    }

    @Test
    fun deenTabRecordsJamaah() {
        rule.onNodeWithTag("tab_deen").performClick()
        settle()
        val chip = hasText("Jama'ah") and hasAnyAncestor(hasTestTag("prayer_asr"))
        rule.onNode(chip).performScrollTo().performClick()
        settle()
        assertEquals(PrayerStatus.JAMAAH, log.prayer(today, Prayer.ASR))
        rule.onNode(chip).assertIsSelected()
    }

    @Test
    fun bodyTabLogsASessionAndPlansDays() {
        rule.onNodeWithTag("tab_body").performClick()
        settle()
        val pull = hasText("Pull") and isSelectable()
        rule.onNode(pull).performScrollTo().performClick()
        settle()
        assertTrue(log.done(today, "gym"))
        assertEquals("Pull", log.text(today, "gym"))
        rule.onNode(pull).assertIsSelected()

        rule.onNodeWithText("Sa").performScrollTo().performClick()
        settle()
        assertTrue(6 in Prefs(rule.activity).gymDays)
        rule.onNodeWithText("Sa").assertIsSelected()
    }
}
