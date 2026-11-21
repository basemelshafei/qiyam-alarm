package com.basem.qiyam

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ScoreTest {
    private fun item(weight: Int, progress: Float, pillar: Pillar = Pillar.DEEN) =
        TodayItem("x", "x", "", pillar, Instant.EPOCH, weight, progress, ItemKind.TOGGLE)

    @Test fun halfDoneIsFifty() = assertEquals(50, Plan.score(listOf(item(10, 1f), item(10, 0f))))

    @Test fun latePrayerCountsHalf() = assertEquals(75, Plan.score(listOf(item(10, 1f), item(10, 0.5f))))

    @Test fun optionalItemsAreABonusButCapAt100() {
        assertEquals(70, Plan.score(listOf(item(10, 1f), item(10, 0f), item(0, 1f))))
        assertEquals(100, Plan.score(listOf(item(10, 1f), item(0, 1f))))
    }

    @Test fun nothingPlannedIsZero() = assertEquals(0, Plan.score(emptyList()))

    @Test fun pillarScoreOnlyCountsThatPillar() =
        assertEquals(100, Plan.pillarScore(listOf(item(10, 1f), item(10, 0f, Pillar.BODY)), Pillar.DEEN))

    @Test fun fastDaysAreMondayAndThursday() {
        assertEquals(true, Plan.isFastDay(java.time.LocalDate.of(2026, 11, 23)))  // Monday
        assertEquals(true, Plan.isFastDay(java.time.LocalDate.of(2026, 11, 26)))  // Thursday
        assertEquals(false, Plan.isFastDay(java.time.LocalDate.of(2026, 11, 21))) // Saturday
    }

    @Test fun durationsReadNaturally() {
        assertEquals("1h 5m", prettyDuration("01:05:00"))
        assertEquals("48 min", prettyDuration("00:48:10"))
        assertEquals(null, prettyDuration(""))
    }
}
