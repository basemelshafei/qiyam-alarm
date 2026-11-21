package com.basem.qiyam

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/** Lyfta parsing and how synced data feeds the day (runs on a simulated phone for real JSON). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class IntegrationsTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    @Test fun parsesLyftaWorkoutsWithSetsRepsWeights() {
        val ws = Lyfta.parse(LyftaSample.json(today))
        assertTrue(ws.isNotEmpty())
        val w = ws.first { it.title == "Push Day" }
        val bench = w.exercises.first { it.name == "Bench Press (Barbell)" }
        assertEquals(3, bench.sets.size)
        assertEquals(8, bench.sets[0].reps)
        assertNotNull(bench.topSet?.weight)
        assertTrue(ws.zipWithNext().all { (a, b) -> !a.date.isBefore(b.date) }) // newest first
    }

    @Test fun recordsAreFlagged() {
        val ws = Lyfta.parse(LyftaSample.json(today))
        assertTrue(ws.any { it.records > 0 })
    }

    @Test fun aLyftaWorkoutTicksOffThatDaysGym() {
        val ws = Lyfta.parse(LyftaSample.json(today))
        Lyfta.save(ctx, LyftaSample.json(today))
        val day = ws.first().date
        val log = DayLog(ctx)
        assertTrue(Plan.gymSource(log, day)!!.contains("from Lyfta"))
        val gym = Plan.items(Prefs(ctx), log, day, zone).first { it.id == "gym" }
        assertEquals(1f, gym.progress)
    }

    @Test fun fitbitStepsFillTheStepsItemPartly() {
        val prefs = Prefs(ctx)
        prefs.healthSyncedAt = System.currentTimeMillis()
        prefs.stepGoal = 8000
        DayLog(ctx).setText(today, "hc_steps", "4000")
        val steps = Plan.items(prefs, DayLog(ctx), today, zone).first { it.id == "steps" }
        assertEquals(0.5f, steps.progress, 0.001f)
    }

    @Test fun noHealthConnectMeansNoStepsItem() {
        assertTrue(Plan.items(Prefs(ctx), DayLog(ctx), today, zone).none { it.id == "steps" })
    }

    @Test fun lyftaMissingKeyIsExplained() {
        assertEquals("Add your Lyfta API key first", Lyfta.sync(ctx))
    }
}
