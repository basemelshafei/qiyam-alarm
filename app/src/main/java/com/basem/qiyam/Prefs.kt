package com.basem.qiyam

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import java.time.LocalDate
import java.time.ZoneId

/**
 * Compose state that every stored value's getter reads and every setter bumps. Any part of the
 * screen that shows stored data redraws the moment it changes, whoever changed it (the screen,
 * the alarm service, or a background thread).
 */
object Changes {
    private val counter = mutableIntStateOf(0)
    fun watch() { counter.intValue }
    fun bump() { counter.intValue++ }
}

/** Settings and long-lived state. */
class Prefs(context: Context) {
    private val app = context.applicationContext
    private val sp: SharedPreferences =
        app.getSharedPreferences("qiyam", Context.MODE_PRIVATE)

    private fun watch() = Changes.watch()

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        sp.edit().apply(block).apply()
        Changes.bump()
    }

    private fun bool(k: String, d: Boolean): Boolean { watch(); return sp.getBoolean(k, d) }
    private fun int(k: String, d: Int): Int { watch(); return sp.getInt(k, d) }
    private fun long(k: String, d: Long): Long { watch(); return sp.getLong(k, d) }
    private fun str(k: String, d: String): String { watch(); return sp.getString(k, d) ?: d }
    private fun dbl(k: String, d: Double): Double {
        watch(); return java.lang.Double.longBitsToDouble(sp.getLong(k, java.lang.Double.doubleToLongBits(d)))
    }

    var enabled: Boolean
        get() = bool("enabled", true)
        set(v) = edit { putBoolean("enabled", v) }

    val hasLocation: Boolean
        get() { watch(); return sp.contains("lat") }

    var lat: Double
        get() = dbl("lat", 51.5074)
        set(v) = edit { putLong("lat", java.lang.Double.doubleToLongBits(v)) }

    var lng: Double
        get() = dbl("lng", -0.1278)
        set(v) = edit { putLong("lng", java.lang.Double.doubleToLongBits(v)) }

    var placeName: String
        get() = str("place", "London (default)")
        set(v) = edit { putString("place", v) }

    var method: Method
        get() = runCatching { Method.valueOf(str("method", Method.MUSLIM_WORLD_LEAGUE.name)) }
            .getOrDefault(Method.MUSLIM_WORLD_LEAGUE)
        set(v) = edit { putString("method", v.name) }

    /** Minutes after the start of the last third to ring (0 = exactly at the start). */
    var offsetMinutes: Int
        get() = int("offset", 0)
        set(v) = edit { putInt("offset", v) }

    /** Gentle heads-up this many minutes before the main alarm (0 = off). */
    var preAlarmMinutes: Int
        get() = int("pre", 10)
        set(v) = edit { putInt("pre", v) }

    var snoozeMinutes: Int
        get() = int("snooze", 5)
        set(v) = edit { putInt("snooze", v) }

    // --- Telegram -------------------------------------------------------------------------

    /** Telegram bot token: encrypted on the phone and never backed up (see [Secrets]). */
    var botToken: String
        get() = Secrets.get(app, "bot")
        set(v) = Secrets.put(app, "bot", v)

    var chatId: Long
        get() = long("chat", 0L)
        set(v) = edit { putLong("chat", v) }

    /** Telegram polling position. Not shown on screen, so it doesn't trigger redraws. */
    var updateOffset: Long
        get() = sp.getLong("upd", 0L)
        set(v) = sp.edit().putLong("upd", v).apply()

    /** Telegram message id of tonight's reminder, so we can edit it when you wake. */
    var activeMessageId: Long
        get() = sp.getLong("msg", 0L)
        set(v) = sp.edit().putLong("msg", v).apply()

    val telegramReady: Boolean get() = botToken.isNotBlank() && chatId != 0L

    // --- Streaks ----------------------------------------------------------------------------

    /** Nights (keyed by the date of their Fajr) on which you got up. */
    val wokeNights: Set<String>
        get() { watch(); return sp.getStringSet("woke", emptySet())!!.toSet() }

    fun markWoke(night: LocalDate) {
        val updated = (wokeNights + night.toString()).sorted().takeLast(400).toSet()
        edit { putStringSet("woke", updated) }
    }

    fun woke(night: LocalDate) = night.toString() in wokeNights

    /** Consecutive nights up to and including the most recent one. */
    fun streak(currentNight: LocalDate): Int {
        val set = wokeNights
        var d = if (currentNight.toString() in set) currentNight else currentNight.minusDays(1)
        var n = 0
        while (d.toString() in set) { n++; d = d.minusDays(1) }
        return n
    }

    fun best(): Int {
        val days = wokeNights.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in days) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run); prev = d
        }
        return best
    }

    /** Rotates through the reminders so you don't see the same one two nights running. */
    fun nextReminderIndex(size: Int): Int {
        val i = sp.getInt("rem", -1) + 1
        sp.edit().putInt("rem", i).apply()
        return Math.floorMod(i, size)
    }

    val zone: ZoneId get() = ZoneId.systemDefault()
}
