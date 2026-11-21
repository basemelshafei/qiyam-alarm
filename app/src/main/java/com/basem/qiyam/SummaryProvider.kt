package com.basem.qiyam

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Process
import java.time.Instant
import java.time.LocalDate

/**
 * Shares a read-only summary of Qiyam with Istiqamah (the dashboard app):
 * content://com.basem.qiyam.summary/summary
 *
 * Only apps signed with the same key as Qiyam may read it, so no other app on the phone can.
 * Nothing can be changed through it.
 */
class SummaryProvider : ContentProvider() {

    companion object {
        val COLUMNS = arrayOf(
            "enabled", "place", "method", "streak", "best", "total",
            "woke_nights", "next_wake", "last_third", "fajr", "maghrib", "telegram",
        )
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, args: Array<out String>?, sort: String?): Cursor {
        val ctx = context ?: throw IllegalStateException("No context")
        val caller = Binder.getCallingUid()
        if (caller != Process.myUid() &&
            ctx.packageManager.checkSignatures(caller, Process.myUid()) != PackageManager.SIGNATURE_MATCH
        ) {
            throw SecurityException("Only Istiqamah can read Qiyam's summary")
        }
        val token = Binder.clearCallingIdentity()
        try {
            val p = Prefs(ctx)
            val now = Instant.now()
            val night = NightCalculator.currentOrNext(p.lat, p.lng, p.method, now, p.zone)
            val (_, wake) = NightCalculator.nextWake(p.lat, p.lng, p.method, p.offsetMinutes, now, p.zone)
            val today = LocalDate.now(p.zone)
            val recent = p.wokeNights.filter { runCatching { !LocalDate.parse(it).isBefore(today.minusDays(60)) }.getOrDefault(false) }
            return MatrixCursor(COLUMNS).apply {
                addRow(
                    arrayOf<Any>(
                        if (p.enabled) 1 else 0, p.placeName, p.method.label,
                        p.streak(night.key(p.zone)), p.best(), p.wokeNights.size,
                        recent.sorted().joinToString(","), wake.toEpochMilli(),
                        night.lastThird.toEpochMilli(), night.fajr.toEpochMilli(), night.maghrib.toEpochMilli(),
                        if (p.telegramReady) 1 else 0,
                    ),
                )
            }
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.item/vnd.com.basem.qiyam.summary"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, args: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, args: Array<out String>?) = 0
}
