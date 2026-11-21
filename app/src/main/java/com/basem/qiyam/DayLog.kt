package com.basem.qiyam

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/** How a prayer went. Stored as its ordinal, so never reorder these. */
enum class PrayerStatus(val label: String, val credit: Float) {
    NONE("—", 0f),
    ON_TIME("On time", 1f),
    JAMAAH("Jama'ah", 1f),
    LATE("Late", 0.5f),
    MISSED("Missed", 0f),
}

/**
 * Day-by-day records: what you did on each date. Small and simple: one SharedPreferences
 * entry per (date, item). Reads are observable by the screen through [Changes].
 */
class DayLog(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("daylog", Context.MODE_PRIVATE)

    private fun key(day: LocalDate, item: String) = "$day|$item"

    fun value(day: LocalDate, item: String): Int {
        Changes.watch(); return sp.getInt(key(day, item), 0)
    }

    fun setValue(day: LocalDate, item: String, v: Int) {
        sp.edit().putInt(key(day, item), v).apply(); Changes.bump()
    }

    fun done(day: LocalDate, item: String) = value(day, item) > 0

    fun toggle(day: LocalDate, item: String) = setValue(day, item, if (done(day, item)) 0 else 1)

    fun text(day: LocalDate, item: String): String {
        Changes.watch(); return sp.getString(key(day, item) + "#t", "") ?: ""
    }

    fun setText(day: LocalDate, item: String, v: String) {
        sp.edit().putString(key(day, item) + "#t", v.trim()).apply(); Changes.bump()
    }

    fun prayer(day: LocalDate, p: Prayer): PrayerStatus =
        PrayerStatus.entries.getOrElse(value(day, p.id)) { PrayerStatus.NONE }

    fun setPrayer(day: LocalDate, p: Prayer, s: PrayerStatus) = setValue(day, p.id, s.ordinal)

    /** Bodyweight in kg for a day, or null. */
    fun weight(day: LocalDate): Float? = text(day, "weight").toFloatOrNull()

    fun setWeight(day: LocalDate, kg: Float?) = setText(day, "weight", kg?.toString() ?: "")

    /** Most recent bodyweights, oldest first, looking back [days] days. */
    fun weights(until: LocalDate, days: Int = 60): List<Pair<LocalDate, Float>> =
        (days - 1 downTo 0).mapNotNull { i -> val d = until.minusDays(i.toLong()); weight(d)?.let { d to it } }
}

enum class Prayer(val id: String, val label: String, val arabic: String) {
    FAJR("fajr", "Fajr", "الفجر"),
    DHUHR("dhuhr", "Dhuhr", "الظهر"),
    ASR("asr", "Asr", "العصر"),
    MAGHRIB("maghrib", "Maghrib", "المغرب"),
    ISHA("isha", "Isha", "العشاء");

    fun time(p: DayPrayers) = when (this) {
        FAJR -> p.fajr; DHUHR -> p.dhuhr; ASR -> p.asr; MAGHRIB -> p.maghrib; ISHA -> p.isha
    }
}

/** Simple gym session types. Stored by name in the log's text for "gym". */
val GymTypes = listOf("Push", "Pull", "Legs", "Upper", "Lower", "Full body", "Cardio", "Sport")
