package com.basem.qiyam

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rings the alarm: fading-in sound, vibration, a full-screen alarm page, and a Telegram
 * message whose buttons work just like the ones on screen. Escalates if you don't respond.
 */
class AlarmService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_UP = "up"
        const val ACTION_SNOOZE = "snooze"
        const val EXTRA_TEST = "test"
        const val EXTRA_SNOOZE = "snooze"

        private const val NOTIF_ID = 1
        private const val NUDGE_AFTER_MS = 3 * 60_000L
        private const val GIVE_UP_AFTER_MS = 10 * 60_000L
        private const val TAG = "QiyamAlarm"

        /** What the alarm screen shows. Null when nothing is ringing. */
        @Volatile var ringing: Ringing? = null
            internal set // tests set it to preview the alarm page

        fun send(ctx: Context, action: String) {
            ctx.startService(Intent(ctx, AlarmService::class.java).setAction(action))
        }
    }

    data class Ringing(
        val night: NightWindow,
        val reminder: Reminder,
        val test: Boolean,
        val startedAt: Instant,
    )

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val polling = AtomicBoolean(false)
    private val fmt get() = DateTimeFormatter.ofPattern("HH:mm").withZone(Prefs(this).zone)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(
                test = intent.getBooleanExtra(EXTRA_TEST, false),
                snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false),
            )
            ACTION_UP -> imUp(fromTelegram = false)
            ACTION_SNOOZE -> snooze(fromTelegram = false)
            else -> if (ringing == null) stopSelf()
        }
        return START_NOT_STICKY
    }

    // --- Ringing ------------------------------------------------------------------------------

    private fun start(test: Boolean, snooze: Boolean) {
        val p = Prefs(this)
        val night = NightCalculator.currentOrNext(p.lat, p.lng, p.method, Instant.now(), p.zone)
        val reminder = ringing?.reminder ?: Reminders.all[p.nextReminderIndex(Reminders.all.size)]
        val r = Ringing(night, reminder, test, Instant.now())
        ringing = r

        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(r),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "qiyam:alarm")
            .apply { acquire(GIVE_UP_AFTER_MS + 60_000L) }

        playSound()
        vibrate()

        // Try to bring the alarm page up directly too (works when the screen is off/locked).
        runCatching {
            startActivity(Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        if (p.telegramReady) {
            Thread {
                runCatching {
                    val tg = Telegram(p.botToken)
                    val id = tg.send(p.chatId, wakeMessage(r, snooze), listOf("✅ I'm up" to "up", "😴 Snooze" to "snooze"))
                    if (!snooze && !test) p.activeMessageId = id
                }.onFailure { Log.w(TAG, "Telegram send failed", it) }
                pollTelegram()
            }.start()
        }

        main.removeCallbacksAndMessages(null)
        main.postDelayed({ nudge() }, NUDGE_AFTER_MS)
        main.postDelayed({ giveUp() }, GIVE_UP_AFTER_MS)
    }

    private fun playSound() {
        stopSound()
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                setVolume(0.05f, 0.05f)
                prepare()
                start()
            }
        }.onFailure { Log.w(TAG, "Couldn't play alarm sound", it) }.getOrNull()

        // Gentle fade-in over ~40 seconds: a soft start, but it won't let you sleep through.
        var step = 0
        val fade = object : Runnable {
            override fun run() {
                val mp = player ?: return
                step++
                val v = (0.05f + step * 0.0475f).coerceAtMost(1f)
                runCatching { mp.setVolume(v, v) }
                if (v < 1f) main.postDelayed(this, 2_000)
            }
        }
        main.postDelayed(fade, 2_000)
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        vibrator = getSystemService(Vibrator::class.java)
        val pattern = longArrayOf(0, 600, 900, 600, 2400)
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) }
    }

    private fun stopSound() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { vibrator?.cancel() }
    }

    // --- Responses ----------------------------------------------------------------------------

    private fun imUp(fromTelegram: Boolean) {
        val r = ringing ?: return finish()
        val p = Prefs(this)
        val now = Instant.now()
        if (!r.test) p.markWoke(r.night.key(p.zone))
        Scheduler.cancelSnooze(this)
        val streak = p.streak(r.night.key(p.zone))
        val left = Duration.between(now, r.night.fajr)

        tg { tg ->
            val msgId = p.activeMessageId
            val text = buildString {
                append(if (r.test) "🧪 <b>Test complete</b>\n" else "✅ <b>Up at ${fmt.format(now)}</b>\n")
                append("Fajr at ${fmt.format(r.night.fajr)} · ${human(left)} of the last third left.\n\n")
                if (!r.test) append("🔥 Streak: <b>$streak</b> night${if (streak == 1) "" else "s"}")
                if (!r.test && streak > 0 && streak == p.best()) append(" · personal best")
                if (!r.test) append("\n\n")
                append("<i>${Telegram.esc(Reminders.wakingDua.arabic)}</i>\n")
                append("May Allah accept from you. 🤲")
            }
            if (msgId != 0L && !r.test) tg.edit(p.chatId, msgId, text) else tg.send(p.chatId, text)
        }
        if (fromTelegram) Log.i(TAG, "Woken via Telegram")
        finish()
    }

    private fun snooze(fromTelegram: Boolean) {
        val r = ringing ?: return finish()
        val p = Prefs(this)
        val at = Instant.now().plusSeconds(p.snoozeMinutes * 60L)
        val latest = r.night.fajr.minusSeconds(5 * 60)
        val wakeAt = if (at.isAfter(latest)) latest else at
        if (wakeAt.isAfter(Instant.now())) Scheduler.scheduleSnooze(this, wakeAt)
        tg { tg ->
            tg.send(
                p.chatId,
                "😴 Snoozed until <b>${fmt.format(wakeAt)}</b>. Fajr is at ${fmt.format(r.night.fajr)}. " +
                    "The doors of the last third are still open.",
            )
        }
        if (fromTelegram) Log.i(TAG, "Snoozed via Telegram")
        finish()
    }

    private fun nudge() {
        val r = ringing ?: return
        val p = Prefs(this)
        tg { tg ->
            tg.send(
                p.chatId,
                "🔔 Still asleep? It's ${fmt.format(Instant.now())}. " +
                    "${human(Duration.between(Instant.now(), r.night.fajr))} until Fajr. Even two rak'ahs count.",
                listOf("✅ I'm up" to "up"),
            )
        }
    }

    private fun giveUp() {
        val r = ringing ?: return
        val p = Prefs(this)
        tg { tg ->
            val text = "🌙 The alarm stopped after 10 minutes. " +
                "If you wake before Fajr (${fmt.format(r.night.fajr)}), there's still time. Tap below when you're up."
            tg.send(p.chatId, text, listOf("✅ I'm up" to "up"))
        }
        // Keep listening for a late "I'm up" tap for a while, quietly.
        stopSound()
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ finish() }, 30 * 60_000L)
    }

    // --- Telegram ------------------------------------------------------------------------------

    private fun tg(block: (Telegram) -> Unit) {
        val p = Prefs(this)
        if (!p.telegramReady) return
        Thread { runCatching { block(Telegram(p.botToken)) }.onFailure { Log.w(TAG, "Telegram", it) } }.start()
    }

    /** Listens for taps on the message's buttons while the alarm is active. */
    private fun pollTelegram() {
        if (!polling.compareAndSet(false, true)) return
        val p = Prefs(this)
        val tg = Telegram(p.botToken)
        try {
            while (ringing != null) {
                val updates = runCatching { tg.updates(p.updateOffset, 20) }.getOrElse {
                    Thread.sleep(5_000); emptyList()
                }
                for (u in updates) {
                    p.updateOffset = u.updateId + 1
                    if (u.callbackId == null || u.chatId != p.chatId) continue
                    when (u.data) {
                        "up" -> { tg.answerCallback(u.callbackId, "Alhamdulillah 🤲"); main.post { imUp(true) } }
                        "snooze" -> { tg.answerCallback(u.callbackId, "Snoozed"); main.post { snooze(true) } }
                    }
                }
            }
        } finally {
            polling.set(false)
        }
    }

    private fun wakeMessage(r: Ringing, snooze: Boolean): String {
        val p = Prefs(this)
        val left = Duration.between(Instant.now(), r.night.fajr)
        val rem = r.reminder
        return buildString {
            if (r.test) append("🧪 <b>Test alarm</b>\n\n")
            if (snooze) {
                append("⏰ <b>Snooze is over.</b> ${human(left)} until Fajr (${fmt.format(r.night.fajr)}).")
                return@buildString
            }
            append("🌙 <b>The last third of the night has begun</b>\n")
            append("Started ${fmt.format(r.night.lastThird)} · Fajr ${fmt.format(r.night.fajr)} · ${human(left)} left\n\n")
            append("<i>${Telegram.esc(rem.arabic)}</i>\n\n")
            append("${Telegram.esc(rem.english)}\n— ${Telegram.esc(rem.source)}")
            if (isFastDay(r.night.key(p.zone))) {
                append("\n\n🍽 Today is a Monday/Thursday fast. Have suhoor before Fajr at ${fmt.format(r.night.fajr)}.")
            }
            val s = p.streak(r.night.key(p.zone))
            if (s > 0) append("\n\n🔥 You're on a $s-night streak. Keep it going.")
        }
    }

    // --- Notification ----------------------------------------------------------------------------

    private fun buildNotification(r: Ringing): Notification {
        val full = PendingIntent.getActivity(
            this, 20, Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        fun action(a: String, rc: Int) = PendingIntent.getService(
            this, rc, Intent(this, AlarmService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, Scheduler.CH_ALARM)
            .setSmallIcon(R.drawable.ic_stat_moon)
            .setContentTitle(if (r.test) "Test: the last third has begun" else "The last third of the night has begun")
            .setContentText("Fajr at ${fmt.format(r.night.fajr)}. Rise, the Lord is near.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(full, true)
            .setContentIntent(full)
            .addAction(0, "I'm up", action(ACTION_UP, 21))
            .addAction(0, "Snooze", action(ACTION_SNOOZE, 22))
            .build()
    }

    private fun finish() {
        main.removeCallbacksAndMessages(null)
        stopSound()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        ringing = null
        AlarmActivity.close()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
        stopSelf()
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        stopSound()
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        ringing = null
        super.onDestroy()
    }
}

fun human(d: Duration): String {
    val m = d.toMinutes().coerceAtLeast(0)
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}
