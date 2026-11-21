package com.basem.qiyam.shots

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.basem.qiyam.AlarmActivity
import com.basem.qiyam.AlarmService
import com.basem.qiyam.MainActivity
import com.basem.qiyam.Method
import com.basem.qiyam.NightCalculator
import com.basem.qiyam.Reminders
import com.basem.qiyam.ui.Motion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.ZoneId

/* Renders Qiyam's screens with sample data into app/build/screenshots (a typical 393dp phone). */

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h1700dp-xxhdpi")
class LightScreenshots {
    init { Motion.live = false }
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun home() { Shots.seed(rule.activity); Shots.save(rule, "qiyam_1_light") }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h1700dp-night-xxhdpi")
class DarkScreenshots {
    init { Motion.live = false }
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun home() { Shots.seed(rule.activity); Shots.save(rule, "qiyam_2_dark") }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class AlarmScreenshot {
    init {
        Motion.live = false
        val night = NightCalculator.currentOrNext(51.5074, -0.1278, Method.MUSLIM_WORLD_LEAGUE, Instant.now(), ZoneId.systemDefault())
        AlarmService.ringing = AlarmService.Ringing(night, Reminders.all[0], test = false, startedAt = Instant.now())
    }
    @get:Rule val rule = createAndroidComposeRule<AlarmActivity>()
    @Test fun alarmPage() = Shots.save(rule, "qiyam_3_alarm")
}
