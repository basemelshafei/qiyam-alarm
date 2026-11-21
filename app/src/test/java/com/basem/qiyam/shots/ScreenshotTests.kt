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

/*
 * Renders every screen with sample data into app/build/screenshots, so each build can be
 * looked at before installing. Pages are rendered extra tall to show them in full.
 * Rendered at a typical modern Android phone size (393dp wide).
 */

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h1700dp-xxhdpi")
class PhoneScreenshots {
    init { Motion.live = false }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun allScreens() {
        Shots.seed(rule.activity)
        Shots.save(rule, "phone_1_today")
        Shots.tab(rule, "tab_deen"); Shots.save(rule, "phone_2_deen")
        Shots.tab(rule, "tab_body"); Shots.save(rule, "phone_3_body")
        Shots.tab(rule, "tab_career"); Shots.save(rule, "phone_4_career")
        Shots.tab(rule, "tab_wealth"); Shots.save(rule, "phone_5_wealth")
        Shots.openQiyam(rule); Shots.save(rule, "phone_6_qiyam")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h1700dp-night-xxhdpi")
class PhoneScreenshotsDark {
    init { Motion.live = false }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun allScreens() {
        Shots.seed(rule.activity)
        Shots.save(rule, "dark_1_today")
        Shots.tab(rule, "tab_deen"); Shots.save(rule, "dark_2_deen")
        Shots.tab(rule, "tab_body"); Shots.save(rule, "dark_3_body")
        Shots.openQiyam(rule); Shots.save(rule, "dark_6_qiyam")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class AlarmScreenshot {
    init {
        Motion.live = false
        val zone = ZoneId.systemDefault()
        val night = NightCalculator.currentOrNext(51.5074, -0.1278, Method.MUSLIM_WORLD_LEAGUE, Instant.now(), zone)
        AlarmService.ringing = AlarmService.Ringing(night, Reminders.all[0], test = false, startedAt = Instant.now())
    }

    @get:Rule val rule = createAndroidComposeRule<AlarmActivity>()

    @Test fun alarmPage() = Shots.save(rule, "phone_7_alarm")
}
