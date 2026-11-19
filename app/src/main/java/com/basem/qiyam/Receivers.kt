package com.basem.qiyam

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.format.DateTimeFormatter

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.createChannels(ctx)
        when (intent.action) {
            Scheduler.ACTION_PRE -> preAlarm(ctx)
            Scheduler.ACTION_WAKE, Scheduler.ACTION_SNOOZE_WAKE -> {
                val test = intent.getBooleanExtra(Scheduler.EXTRA_TEST, false)
                val snooze = intent.action == Scheduler.ACTION_SNOOZE_WAKE && !test
                ContextCompat.startForegroundService(
                    ctx,
                    Intent(ctx, AlarmService::class.java)
                        .setAction(AlarmService.ACTION_START)
                        .putExtra(AlarmService.EXTRA_TEST, test)
                        .putExtra(AlarmService.EXTRA_SNOOZE, snooze),
                )
                // A real wake: line up tomorrow night straight away.
                if (intent.action == Scheduler.ACTION_WAKE) {
                    Scheduler.scheduleNext(ctx, Instant.now().plusSeconds(60))
                }
            }
        }
    }

    private fun preAlarm(ctx: Context) {
        val p = Prefs(ctx)
        val night = NightCalculator.currentOrNext(p.lat, p.lng, p.method, Instant.now(), p.zone)
        val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(p.zone)
        val n = NotificationCompat.Builder(ctx, Scheduler.CH_PRE)
            .setSmallIcon(R.drawable.ic_stat_moon)
            .setContentTitle("The last third is near")
            .setContentText("Begins ${fmt.format(night.lastThird)} · Fajr ${fmt.format(night.fajr)}. A good moment for wudu.")
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    ctx, 10, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(2, n) }
    }
}

/** Re-arms the alarm after a reboot, app update, or a clock/time-zone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.createChannels(ctx)
        Scheduler.scheduleNext(ctx)
    }
}
