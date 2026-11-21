package com.basem.qiyam.shots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.basem.qiyam.DayLog
import com.basem.qiyam.Prayer
import com.basem.qiyam.PrayerStatus
import com.basem.qiyam.Prefs
import java.io.File
import java.time.LocalDate

/** Shared helpers: realistic sample data and saving what's on screen as a PNG. */
object Shots {
    val dir = File("build/screenshots").apply { mkdirs() }

    fun seed(activity: ComponentActivity) {
        val prefs = Prefs(activity)
        val log = DayLog(activity)
        val today = LocalDate.now(prefs.zone)
        prefs.lat = 51.5074; prefs.lng = -0.1278; prefs.placeName = "London"
        prefs.quranCurrent = "Al-Mulk 1–15"
        prefs.gymDays = setOf(1, 2, 4, 5)

        val statuses = listOf(PrayerStatus.JAMAAH, PrayerStatus.ON_TIME, PrayerStatus.ON_TIME, PrayerStatus.LATE, PrayerStatus.JAMAAH)
        for (i in 1L..20L) {
            val d = today.minusDays(i)
            Prayer.entries.forEachIndexed { k, p ->
                val s = if ((i + k) % 9 == 0L) PrayerStatus.MISSED else statuses[((i + k) % statuses.size).toInt()]
                log.setPrayer(d, p, s)
            }
            if (i % 4 != 0L) log.setValue(d, "adhkar_am", 1)
            if (i % 3 != 0L) log.setValue(d, "adhkar_pm", 1)
            if (i % 2 == 0L) log.setValue(d, "quran_new", 1)
            if (i % 5 != 0L) log.setValue(d, "quran_rev", 1)
            if (d.dayOfWeek.value in prefs.gymDays && i % 7 != 3L) {
                log.setValue(d, "gym", 1); log.setText(d, "gym", listOf("Push", "Pull", "Legs", "Upper")[(i % 4).toInt()])
            }
            if (d.dayOfWeek.value == 1 || d.dayOfWeek.value == 4) log.setValue(d, "fast", 1)
            if (i <= 6) prefs.markWoke(d)
        }
        prefs.markWoke(today)
        for (i in 0L..60L step 3) log.setWeight(today.minusDays(i), 79.4f + i * 0.045f)
        // Connected apps: Lyfta workouts and a week of Fitbit data.
        com.basem.qiyam.Lyfta.save(activity, com.basem.qiyam.LyftaSample.json(today))
        prefs.lyftaKey = "sample"; prefs.lyftaSyncedAt = System.currentTimeMillis()
        prefs.healthSyncedAt = System.currentTimeMillis()
        for (i in 0L..6L) {
            val d = today.minusDays(i)
            log.setText(d, "hc_steps", (if (i == 0L) 6240 else 7000 + (i * 911) % 4000).toString())
            log.setText(d, "hc_sleep_min", (370 + (i * 17) % 60).toString())
            log.setText(d, "hc_rhr", (56 + i % 4).toString())
        }
        log.setPrayer(today, Prayer.FAJR, PrayerStatus.JAMAAH)
        log.setValue(today, "adhkar_am", 1)
    }

    fun save(rule: AndroidComposeTestRule<out ActivityScenarioRule<*>, *>, name: String) {
        rule.waitForIdle()
        val bmp = runCatching { rule.onRoot().captureToImage().asAndroidBitmap() }.getOrElse {
            // Fallback: draw the window into a bitmap.
            var out: Bitmap? = null
            rule.runOnUiThread {
                val v = (rule.activity as android.app.Activity).window.decorView.rootView
                out = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(Canvas(it)) }
            }
            out!!
        }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    fun tab(rule: AndroidComposeTestRule<*, *>, tag: String) {
        rule.onNodeWithTag(tag).performClick(); rule.waitForIdle()
    }

    fun openQiyam(rule: AndroidComposeTestRule<*, *>) {
        tab(rule, "tab_deen")
        rule.onNodeWithText("Qiyam alarm").performScrollTo().performClick(); rule.waitForIdle()
    }
}
