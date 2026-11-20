package com.basem.qiyam

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import java.time.Instant

object Scheduler {
    const val ACTION_PRE = "com.basem.qiyam.PRE"
    const val ACTION_WAKE = "com.basem.qiyam.WAKE"
    const val ACTION_SNOOZE_WAKE = "com.basem.qiyam.SNOOZE_WAKE"
    const val EXTRA_TEST = "test"

    const val CH_ALARM = "alarm"
    const val CH_PRE = "pre"
    const val CH_INFO = "info"

    private const val RC_WAKE = 1
    private const val RC_PRE = 2
    private const val RC_SNOOZE = 3
    private const val RC_SHOW = 4

    private fun am(ctx: Context) = ctx.getSystemService(AlarmManager::class.java)

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am(ctx).canScheduleExactAlarms()

    private fun pi(ctx: Context, action: String, rc: Int, test: Boolean = false): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, rc,
            Intent(ctx, AlarmReceiver::class.java).setAction(action).putExtra(EXTRA_TEST, test),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun showApp(ctx: Context): PendingIntent =
        PendingIntent.getActivity(
            ctx, RC_SHOW, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * Schedules tonight's (or the next) wake alarm and its heads-up. Safe to call any time:
     * it always replaces what was there. Returns the wake time, or null if disabled.
     */
    fun scheduleNext(ctx: Context, now: Instant = Instant.now()): Instant? {
        val p = Prefs(ctx)
        val am = am(ctx)
        am.cancel(pi(ctx, ACTION_WAKE, RC_WAKE))
        am.cancel(pi(ctx, ACTION_PRE, RC_PRE))
        if (!p.enabled) return null

        val (_, wake) = NightCalculator.nextWake(p.lat, p.lng, p.method, p.offsetMinutes, now, p.zone)

        // setAlarmClock = a real alarm: shows the alarm icon, survives Doze, can't be deferred.
        alarmClock(ctx, wake, pi(ctx, ACTION_WAKE, RC_WAKE))

        if (p.preAlarmMinutes > 0) {
            val pre = wake.minusSeconds(p.preAlarmMinutes * 60L)
            if (pre.isAfter(now)) exact(ctx, pre, pi(ctx, ACTION_PRE, RC_PRE))
        }
        return wake
    }

    fun scheduleSnooze(ctx: Context, at: Instant) = alarmClock(ctx, at, pi(ctx, ACTION_SNOOZE_WAKE, RC_SNOOZE))

    /** A real alarm if Android allows it; otherwise the closest thing, rather than crashing. */
    private fun alarmClock(ctx: Context, at: Instant, op: PendingIntent) {
        try {
            am(ctx).setAlarmClock(AlarmManager.AlarmClockInfo(at.toEpochMilli(), showApp(ctx)), op)
        } catch (e: SecurityException) {
            am(ctx).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), op)
        }
    }

    fun cancelSnooze(ctx: Context) = am(ctx).cancel(pi(ctx, ACTION_SNOOZE_WAKE, RC_SNOOZE))

    /** Fires a full test alarm a few seconds from now without touching tonight's schedule. */
    fun scheduleTest(ctx: Context, seconds: Long) {
        alarmClock(ctx, Instant.now().plusSeconds(seconds), pi(ctx, ACTION_SNOOZE_WAKE, RC_SNOOZE, test = true))
    }

    private fun exact(ctx: Context, at: Instant, op: PendingIntent) {
        val am = am(ctx)
        if (canExact(ctx) && runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), op)
            }.isSuccess
        ) {
            return
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), op)
        }
    }

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val alarm = NotificationChannel(CH_ALARM, "Last-third alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "The wake-up alarm for the last third of the night"
            setSound(null, null) // the service plays the sound itself, with a fade-in
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val pre = NotificationChannel(CH_PRE, "Gentle heads-up", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "A soft reminder a few minutes before the alarm"
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build(),
            )
        }
        val info = NotificationChannel(CH_INFO, "Status", NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannels(listOf(alarm, pre, info))
    }
}
