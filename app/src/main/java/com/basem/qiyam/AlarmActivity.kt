package com.basem.qiyam

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Motion
import com.basem.qiyam.ui.Sky
import com.basem.qiyam.ui.NightDial
import com.basem.qiyam.ui.QiyamTheme
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

/** The full-screen page shown over the lock screen while the alarm rings. */
class AlarmActivity : ComponentActivity() {

    companion object {
        private var current: WeakReference<AlarmActivity>? = null
        fun close() { current?.get()?.finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val r = AlarmService.ringing
        if (r == null) { finish(); return }

        setContent {
            QiyamTheme {
                AlarmScreen(
                    r,
                    onUp = {
                        AlarmService.send(this, AlarmService.ACTION_UP)
                        // Dismiss the keyguard so you land straight on your phone, ready to pray.
                        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(this, null)
                        finish()
                    },
                    onSnooze = { AlarmService.send(this, AlarmService.ACTION_SNOOZE); finish() },
                )
            }
        }
    }

    override fun onDestroy() {
        if (current?.get() === this) current = null
        super.onDestroy()
    }
}

@Composable
private fun AlarmScreen(r: AlarmService.Ringing, onUp: () -> Unit, onSnooze: () -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    val fmt = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(zone) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (Motion.live) { now = Instant.now(); delay(1000) } }

    Column(
        Modifier.fillMaxSize().background(Sky.gradient).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(fmt.format(now), color = Sky.Text, fontSize = 64.sp, fontWeight = FontWeight.Light)
        Text(
            if (r.test) "Test alarm" else "The last third of the night has begun",
            color = Sky.Gold, fontSize = 16.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        NightDial(r.night.progress(now), Modifier.widthIn(max = 420.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 6.dp)) {
                Text(human(Duration.between(now, r.night.fajr)), color = Sky.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("until Fajr · ${fmt.format(r.night.fajr)}", color = Sky.Muted, fontSize = 13.sp)
            }
        }
        if (isFastDay(r.night.key(zone))) {
            Text(
                "Fast day: suhoor before ${fmt.format(r.night.fajr)}",
                color = Sky.Gold, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(20.dp))

        Text(
            r.reminder.arabic, color = Sky.Text, fontSize = 21.sp, lineHeight = 36.sp, textAlign = TextAlign.Center,
            style = androidx.compose.ui.text.TextStyle(textDirection = TextDirection.Rtl),
        )
        Spacer(Modifier.height(10.dp))
        Text(r.reminder.english, color = Sky.Muted, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
        Text("— ${r.reminder.source}", color = Sky.Gold.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onUp,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Sky.Gold, contentColor = Sky.Ink),
        ) { Text("I'm up  ·  Alhamdulillah", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) { Text("Snooze", color = Sky.Muted, fontSize = 16.sp) }

        Spacer(Modifier.height(24.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("On waking", color = Sky.Muted, fontSize = 12.sp)
            Text(
                Reminders.wakingDua.arabic, color = Sky.Text.copy(alpha = 0.85f), fontSize = 17.sp,
                textAlign = TextAlign.Center, lineHeight = 28.sp,
            )
        }
    }
}
