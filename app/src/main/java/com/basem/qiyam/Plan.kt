package com.basem.qiyam

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class Pillar(val label: String, val emoji: String) {
    DEEN("Deen", "🕌"),
    BODY("Body", "💪"),
    CAREER("Career", "🚀"),
    WEALTH("Wealth", "📈"),
}

/** How an item is completed when tapped on the Today screen. */
enum class ItemKind { TOGGLE, PRAYER, QIYAM, GYM }

/**
 * One thing to do today.
 * @param weight how much it counts toward the daily score (0 = optional today).
 * @param progress 0..1, how complete it is.
 */
data class TodayItem(
    val id: String,
    val title: String,
    val note: String,
    val pillar: Pillar,
    val time: Instant,
    val weight: Int,
    val progress: Float,
    val kind: ItemKind,
    val prayer: Prayer? = null,
)

/** Builds the day's timeline, ordered around the prayer times, and scores it. */
object Plan {

    // Weights: what matters most counts most. Obligatory prayers lead.
    const val W_PRAYER = 10
    const val W_QIYAM = 8
    const val W_FAST = 8
    const val W_ADHKAR = 4
    const val W_QURAN = 5
    const val W_GYM = 8

    fun isFastDay(day: LocalDate) = day.dayOfWeek == DayOfWeek.MONDAY || day.dayOfWeek == DayOfWeek.THURSDAY

    fun items(prefs: Prefs, log: DayLog, day: LocalDate, zone: ZoneId): List<TodayItem> {
        val p = NightCalculator.prayers(prefs.lat, prefs.lng, prefs.method, day, zone)
        val night = NightCalculator.nightStarting(prefs.lat, prefs.lng, prefs.method, day.minusDays(1), zone)
        val gymDay = day.dayOfWeek.value in prefs.gymDays
        val list = mutableListOf<TodayItem>()

        fun add(id: String, title: String, note: String, pillar: Pillar, time: Instant, weight: Int, kind: ItemKind = ItemKind.TOGGLE) =
            list.add(TodayItem(id, title, note, pillar, time, weight, if (log.done(day, id)) 1f else 0f, kind))

        // The night before Fajr belongs to this day.
        list.add(
            TodayItem(
                "qiyam", "Qiyam", "Last third from ${hm(night.lastThird, zone)}", Pillar.DEEN,
                night.lastThird, W_QIYAM, if (prefs.woke(day) || log.done(day, "qiyam")) 1f else 0f, ItemKind.QIYAM,
            ),
        )
        if (isFastDay(day)) {
            add("fast", "Sunnah fast", "${day.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}: suhoor before ${hm(p.fajr, zone)}", Pillar.DEEN, p.fajr.minusSeconds(1), W_FAST)
        }
        Prayer.entries.forEach { pr ->
            val s = log.prayer(day, pr)
            list.add(
                TodayItem(
                    pr.id, pr.label, if (s == PrayerStatus.NONE) hm(pr.time(p), zone) else s.label,
                    Pillar.DEEN, pr.time(p), W_PRAYER, s.credit, ItemKind.PRAYER, pr,
                ),
            )
        }
        add("adhkar_am", "Morning adhkar", "After Fajr, before sunrise ${hm(p.sunrise, zone)}", Pillar.DEEN, p.fajr.plus(Duration.ofMinutes(10)), W_ADHKAR)
        add("adhkar_pm", "Evening adhkar", "After Asr, before Maghrib", Pillar.DEEN, p.asr.plus(Duration.ofMinutes(10)), W_ADHKAR)
        val quranNow = prefs.quranCurrent.ifBlank { "Set what you're memorising in Deen" }
        add("quran_new", "Quran: memorise", quranNow, Pillar.DEEN, p.maghrib.plus(Duration.ofMinutes(20)), W_QURAN)
        add("quran_rev", "Quran: revise", "Keep what you've memorised", Pillar.DEEN, p.isha.plus(Duration.ofMinutes(20)), W_QURAN)

        val gymType = log.text(day, "gym")
        list.add(
            TodayItem(
                "gym", if (gymDay) "Gym" else "Gym (rest day)",
                if (gymType.isNotBlank()) gymType else if (gymDay) "Tap to log your session" else "Optional today",
                Pillar.BODY, p.asr.plus(Duration.ofMinutes(45)), if (gymDay) W_GYM else 0,
                if (log.done(day, "gym")) 1f else 0f, ItemKind.GYM,
            ),
        )
        return list.sortedBy { it.time }
    }

    /** 0–100. Items with weight 0 don't count against you; extra effort on them is a bonus up to 100. */
    fun score(items: List<TodayItem>): Int {
        val total = items.sumOf { it.weight }
        if (total == 0) return 0
        val earned = items.sumOf { (it.weight * it.progress).toDouble() } +
            items.filter { it.weight == 0 }.sumOf { (W_GYM / 2 * it.progress).toDouble() }
        return ((earned / total) * 100).toInt().coerceIn(0, 100)
    }

    fun scoreFor(prefs: Prefs, log: DayLog, day: LocalDate, zone: ZoneId) = score(items(prefs, log, day, zone))

    fun pillarScore(items: List<TodayItem>, pillar: Pillar) = score(items.filter { it.pillar == pillar })

    private fun hm(t: Instant, zone: ZoneId) =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(t)
}
