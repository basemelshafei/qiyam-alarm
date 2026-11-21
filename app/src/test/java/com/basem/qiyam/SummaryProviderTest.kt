package com.basem.qiyam

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The summary Istiqamah reads: streak, nights, tonight's times. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SummaryProviderTest {
    @Test fun sharesStreakAndTonightsTimes() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = Prefs(ctx)
        val today = LocalDate.now(prefs.zone)
        for (i in 0L..2L) prefs.markWoke(today.minusDays(i))
        val provider = Robolectric.setupContentProvider(SummaryProvider::class.java, "com.basem.qiyam.summary")
        provider.query(Uri.parse("content://com.basem.qiyam.summary/summary"), null, null, null, null).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(1, c.getInt(c.getColumnIndexOrThrow("enabled")))
            assertEquals(3, c.getInt(c.getColumnIndexOrThrow("total")))
            assertTrue(c.getString(c.getColumnIndexOrThrow("woke_nights")).contains(today.toString()))
            assertTrue(c.getLong(c.getColumnIndexOrThrow("last_third")) < c.getLong(c.getColumnIndexOrThrow("fajr")))
        }
    }
}
