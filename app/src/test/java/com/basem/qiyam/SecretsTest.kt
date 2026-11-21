package com.basem.qiyam

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SecretsTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    @Test fun telegramTokenMovesOutOfOrdinarySettings() {
        ctx.getSharedPreferences("qiyam", Context.MODE_PRIVATE).edit().putString("bot", "123:abc").commit()
        Secrets.migrate(ctx)
        assertEquals("123:abc", Prefs(ctx).botToken)
        assertNull(ctx.getSharedPreferences("qiyam", Context.MODE_PRIVATE).getString("bot", null))
    }
}
