package com.basem.qiyam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class NightTest {
    private val london = ZoneId.of("Europe/London")

    @Test fun lastThirdIsTwoThirdsThroughTheNight() {
        val n = NightWindow(Instant.parse("2026-11-19T16:00:00Z"), Instant.parse("2026-11-20T06:00:00Z"))
        assertEquals(Instant.parse("2026-11-20T01:20:00Z"), n.lastThird) // 14h night → 4h40m third
        assertEquals(Instant.parse("2026-11-19T23:00:00Z"), n.midnight)
    }

    @Test fun londonNovemberIsSane() {
        val n = NightCalculator.nightStarting(51.5074, -0.1278, Method.MUSLIM_WORLD_LEAGUE, LocalDate.of(2026, 11, 19), london)
        val hours = n.length.toMinutes() / 60.0
        assertTrue("night length $hours h", hours in 12.5..15.0)
        assertTrue(n.lastThird.isAfter(n.midnight) && n.lastThird.isBefore(n.fajr))
    }

    @Test fun londonMidsummerStillHasAFajr() {
        val n = NightCalculator.nightStarting(51.5074, -0.1278, Method.MUSLIM_WORLD_LEAGUE, LocalDate.of(2027, 6, 21), london)
        assertTrue(n.length > Duration.ofHours(4))
    }

    @Test fun nextWakeIsAlwaysInTheFuture() {
        var t = Instant.parse("2026-11-19T00:00:00Z")
        repeat(96) {
            val (_, wake) = NightCalculator.nextWake(51.5, -0.12, Method.MUSLIM_WORLD_LEAGUE, 0, t, london)
            assertTrue(wake.isAfter(t))
            assertTrue(Duration.between(t, wake) < Duration.ofHours(25))
            t = t.plus(Duration.ofMinutes(30))
        }
    }

    @Test fun wakeOffsetNeverPassesFajr() {
        val n = NightWindow(Instant.parse("2026-06-21T20:20:00Z"), Instant.parse("2026-06-22T01:00:00Z"))
        assertEquals(n.fajr.minus(Duration.ofMinutes(10)), NightCalculator.wakeTime(n, 120))
    }
}
