package com.basem.qiyam.shots

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.basem.qiyam.Prefs
import java.io.File
import java.time.LocalDate

/** Sample data and saving what's on screen as a PNG. */
object Shots {
    val dir = File("build/screenshots").apply { mkdirs() }

    fun seed(activity: ComponentActivity) {
        val prefs = Prefs(activity)
        val today = LocalDate.now(prefs.zone)
        prefs.lat = 51.5074; prefs.lng = -0.1278; prefs.placeName = "London"
        for (i in 0L..6L) prefs.markWoke(today.minusDays(i))
        for (i in 9L..12L) prefs.markWoke(today.minusDays(i))
    }

    fun save(rule: AndroidComposeTestRule<out ActivityScenarioRule<*>, *>, name: String) {
        rule.waitForIdle()
        val bmp = runCatching { rule.onRoot().captureToImage().asAndroidBitmap() }.getOrElse {
            var out: Bitmap? = null
            rule.runOnUiThread {
                val v = (rule.activity as android.app.Activity).window.decorView.rootView
                out = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888).also { v.draw(Canvas(it)) }
            }
            out!!
        }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
