package com.basem.qiyam

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.ZoneId

/** Everything the app remembers, in one place. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("qiyam", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    val hasLocation: Boolean
        get() = sp.contains("lat")

    var lat: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong("lat", java.lang.Double.doubleToLongBits(51.5074)))
        set(v) = sp.edit().putLong("lat", java.lang.Double.doubleToLongBits(v)).apply()

    var lng: Double
        get() = java.lang.Double.longBitsToDouble(sp.getLong("lng", java.lang.Double.doubleToLongBits(-0.1278)))
        set(v) = sp.edit().putLong("lng", java.lang.Double.doubleToLongBits(v)).apply()

    var placeName: String
        get() = sp.getString("place", "London (default)") ?: ""
        set(v) = sp.edit().putString("place", v).apply()

    var method: Method
        get() = runCatching { Method.valueOf(sp.getString("method", Method.MUSLIM_WORLD_LEAGUE.name)!!) }
            .getOrDefault(Method.MUSLIM_WORLD_LEAGUE)
        set(v) = sp.edit().putString("method", v.name).apply()

    /** Minutes after the start of the last third to ring (0 = exactly at the start). */
    var offsetMinutes: Int
        get() = sp.getInt("offset", 0)
        set(v) = sp.edit().putInt("offset", v).apply()

    /** Gentle heads-up this many minutes before the main alarm (0 = off). */
    var preAlarmMinutes: Int
        get() = sp.getInt("pre", 10)
        set(v) = sp.edit().putInt("pre", v).apply()

    var snoozeMinutes: Int
        get() = sp.getInt("snooze", 5)
        set(v) = sp.edit().putInt("snooze", v).apply()

    // --- Telegram -------------------------------------------------------------------------

    var botToken: String
        get() = sp.getString("bot", "") ?: ""
        set(v) = sp.edit().putString("bot", v.trim()).apply()

    var chatId: Long
        get() = sp.getLong("chat", 0L)
        set(v) = sp.edit().putLong("chat", v).apply()

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
    var wokeNights: Set<String>
        get() = sp.getStringSet("woke", emptySet())!!.toSet()
        private set(v) = sp.edit().putStringSet("woke", v).apply()

    fun markWoke(night: LocalDate) {
        wokeNights = (wokeNights + night.toString())
            .sorted().takeLast(400).toSet()
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
