package com.basem.qiyam

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/** One Islamic night: from Maghrib until the following Fajr. */
data class NightWindow(val maghrib: Instant, val fajr: Instant) {
    val length: Duration get() = Duration.between(maghrib, fajr)

    /** Start of the last third: Fajr minus a third of the night. */
    val lastThird: Instant get() = fajr.minus(length.dividedBy(3))

    /** Islamic midnight: halfway between Maghrib and Fajr. */
    val midnight: Instant get() = maghrib.plus(length.dividedBy(2))

    /** Start of the second third. */
    val secondThird: Instant get() = maghrib.plus(length.dividedBy(3))

    operator fun contains(t: Instant) = !t.isBefore(maghrib) && t.isBefore(fajr)

    /** 0.0 at Maghrib, 1.0 at Fajr (clamped). */
    fun progress(t: Instant): Float {
        val total = length.toMillis().toDouble()
        if (total <= 0) return 0f
        return (Duration.between(maghrib, t).toMillis() / total).coerceIn(0.0, 1.0).toFloat()
    }

    /** The date the night "belongs" to, keyed by the morning of its Fajr. Used for streaks. */
    fun key(zone: ZoneId): LocalDate = fajr.atZone(zone).toLocalDate()
}

/** Calculation methods offered in settings. Names map to the adhan library's enum. */
enum class Method(val label: String) {
    MUSLIM_WORLD_LEAGUE("Muslim World League"),
    MOON_SIGHTING_COMMITTEE("Moonsighting Committee (common in UK)"),
    UMM_AL_QURA("Umm al-Qura (Makkah)"),
    EGYPTIAN("Egyptian General Authority"),
    KARACHI("Karachi (Univ. of Islamic Sciences)"),
    NORTH_AMERICA("ISNA (North America)"),
    DUBAI("Dubai"),
    KUWAIT("Kuwait"),
    QATAR("Qatar"),
    SINGAPORE("Singapore");

    fun toAdhan(): CalculationMethod = CalculationMethod.valueOf(name)
}

object NightCalculator {

    private fun prayerTimes(lat: Double, lng: Double, method: Method, day: LocalDate, zone: ZoneId): PrayerTimes {
        val params = method.toAdhan().parameters
        // At UK latitudes in summer the sun never gets 18° below the horizon, so a plain
        // angle-based Fajr can be missing or absurd. "Seventh of the night" keeps it sane.
        params.highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        // Noon local time, so the library's calendar conversion can't slip to the wrong day.
        val noon = Date.from(day.atTime(12, 0).atZone(zone).toInstant())
        return PrayerTimes(Coordinates(lat, lng), DateComponents.from(noon), params)
    }

    /** The night that starts with Maghrib on [day]. */
    fun nightStarting(lat: Double, lng: Double, method: Method, day: LocalDate, zone: ZoneId): NightWindow {
        val evening = prayerTimes(lat, lng, method, day, zone)
        val morning = prayerTimes(lat, lng, method, day.plusDays(1), zone)
        return NightWindow(evening.maghrib.toInstant(), morning.fajr.toInstant())
    }

    /** The night we're in now, or (during daytime) the coming night. */
    fun currentOrNext(lat: Double, lng: Double, method: Method, now: Instant, zone: ZoneId): NightWindow {
        val today = now.atZone(zone).toLocalDate()
        for (offset in -1L..1L) {
            val n = nightStarting(lat, lng, method, today.plusDays(offset), zone)
            if (now.isBefore(n.fajr)) return n
        }
        return nightStarting(lat, lng, method, today.plusDays(1), zone)
    }

    /**
     * The next wake-up moment strictly after [now]: start of the last third plus the
     * user's offset. Returns the night it belongs to together with the time.
     */
    fun nextWake(
        lat: Double, lng: Double, method: Method, offsetMinutes: Int, now: Instant, zone: ZoneId,
    ): Pair<NightWindow, Instant> {
        val today = now.atZone(zone).toLocalDate()
        for (offset in -1L..2L) {
            val n = nightStarting(lat, lng, method, today.plusDays(offset), zone)
            val wake = wakeTime(n, offsetMinutes)
            if (wake.isAfter(now.plusSeconds(5))) return n to wake
        }
        val n = nightStarting(lat, lng, method, today.plusDays(3), zone)
        return n to wakeTime(n, offsetMinutes)
    }

    /** Wake time, never later than 10 minutes before Fajr. */
    fun wakeTime(n: NightWindow, offsetMinutes: Int): Instant {
        val wanted = n.lastThird.plus(Duration.ofMinutes(offsetMinutes.toLong()))
        val latest = n.fajr.minus(Duration.ofMinutes(10))
        return if (wanted.isAfter(latest)) latest else wanted
    }
}
