package com.basem.qiyam

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.basem.qiyam.ui.Motion
import com.basem.qiyam.ui.Badge
import com.basem.qiyam.ui.G
import com.basem.qiyam.ui.Glyph
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.Rings
import com.basem.qiyam.ui.Sky
import com.basem.qiyam.ui.NightDial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The Qiyam alarm page: tonight's dial, alarm status, setup checklist, settings, Telegram. */
@Composable
fun QiyamScreen(onBack: (() -> Unit)?) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    // Bumped whenever something changes, and on resume, so everything re-reads its state.
    // It is passed to each card as a parameter: Compose skips redrawing a card whose
    // parameters haven't changed, and Prefs itself isn't observable.
    var version by remember { mutableIntStateOf(0) }
    fun changed() { Scheduler.scheduleNext(ctx); version++ }

    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (Motion.live) { now = Instant.now(); delay(1000) } }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) changed() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) { if (toast != null) { delay(3500); toast = null } }

    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) fetchLocation(ctx, prefs) { msg -> toast = msg; changed() } else toast = "Location not granted. Using ${prefs.placeName}."
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { changed() }


    val seen = version // reading it makes this screen redraw after any change
    if (seen < 0) return
    val zone = prefs.zone
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val dayFmt = DateTimeFormatter.ofPattern("EEE HH:mm").withZone(zone)
    val night = NightCalculator.currentOrNext(prefs.lat, prefs.lng, prefs.method, now, zone)
    val (_, wake) = NightCalculator.nextWake(prefs.lat, prefs.lng, prefs.method, prefs.offsetMinutes, now, zone)
    val inNight = now in night

    Box(Modifier.fillMaxSize().background(Night.sky)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding().imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Header
                if (onBack != null) Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Glyph(G.CHEVRON, Night.Gold, 20.dp, Modifier.graphicsLayer { rotationZ = 180f })
                        Text("Back", color = Night.Gold, fontSize = 17.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Qiyam", color = Night.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                        Text(
                            "${prefs.placeName} · ${prefs.method.label.substringBefore(" (")}",
                            color = Night.Muted, fontSize = 13.sp,
                            modifier = Modifier.clickable { locationPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                        )
                    }
                    Switch(
                        checked = prefs.enabled,
                        onCheckedChange = { prefs.enabled = it; changed() },
                        colors = SwitchDefaults.colors(checkedTrackColor = Night.Good, checkedThumbColor = Night.OnAccent, checkedBorderColor = Night.Good),
                    )
                }

                // The night dial, always on a night-sky card
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Sky.gradient).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // The night dial
                    NightDial(if (inNight) night.progress(now) else null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
                            when {
                                !inNight -> {
                                    Text("Tonight's last third", color = Sky.Muted, fontSize = 13.sp)
                                    Text(fmt.format(night.lastThird), color = Sky.Text, fontSize = 40.sp, fontWeight = FontWeight.Light)
                                }
                                now.isBefore(night.lastThird) -> {
                                    Text("Last third begins in", color = Sky.Muted, fontSize = 13.sp)
                                    Text(countdown(java.time.Duration.between(now, night.lastThird)), color = Sky.Text, fontSize = 40.sp, fontWeight = FontWeight.Light)
                                }
                                else -> {
                                    Text("The last third is now", color = Sky.Gold, fontSize = 14.sp)
                                    Text(countdown(java.time.Duration.between(now, night.fajr)), color = Sky.Text, fontSize = 40.sp, fontWeight = FontWeight.Light)
                                    Text("until Fajr", color = Sky.Muted, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Times
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TimeCell("Maghrib", fmt.format(night.maghrib))
                        TimeCell("Midnight", fmt.format(night.midnight))
                        TimeCell("Last third", fmt.format(night.lastThird), highlight = true)
                        TimeCell("Fajr", fmt.format(night.fajr))
                    }
                }

                // Alarm status
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Badge(G.BELL, if (prefs.enabled) Night.Warn else Night.Muted, 36.dp)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (prefs.enabled) "Alarm set for ${dayFmt.format(wake)}" else "Alarm is off",
                                color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                            )
                            Text(
                                if (prefs.enabled) buildString {
                                    append(if (prefs.offsetMinutes == 0) "At the start of the last third" else "${prefs.offsetMinutes} min into the last third")
                                    if (prefs.preAlarmMinutes > 0) append(" · heads-up ${prefs.preAlarmMinutes} min before")
                                    if (prefs.telegramReady) append(" · Telegram on")
                                } else "Turn on the switch above to be woken each night",
                                color = Night.Muted, fontSize = 13.sp,
                            )
                        }
                    }
                }

                // Streak
                StreakCard(prefs, night.key(zone), seen)

                // Setup checklist: only shows what still needs doing
                Checklist(ctx, prefs, seen, onNotif = {
                    if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                }, onLocation = { locationPerm.launch(Manifest.permission.ACCESS_COARSE_LOCATION) })

                // Settings
                SettingsCard(prefs, seen, onChanged = { changed() })

                // Telegram
                TelegramCard(prefs, seen, onChanged = { changed() }, onToast = { toast = it })

                // Test
                OutlinedButton(
                    onClick = {
                        Scheduler.scheduleTest(ctx, 10)
                        toast = "Test alarm in 10 seconds. Lock your phone to see the full experience."
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) { Text("Test the full alarm in 10 seconds", color = Night.Gold) }

                Text(
                    "Times are calculated on your phone. Check them against your local mosque, and pick the method that matches.",
                    color = Night.Muted, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            }
        }

        toast?.let {
            Box(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Night.CardEdge).padding(14.dp),
            ) { Text(it, color = Night.Text, fontSize = 14.sp) }
        }
    }
}

private fun countdown(d: java.time.Duration): String {
    val s = d.seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

// ------------------------------------------------------------------------------------------------

@Composable
internal fun Card(content: @Composable ColumnScope.() -> Unit) {
    // iOS inset-grouped card: no border, soft corners, sits on the grey grouped background.
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Night.Card).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
internal fun SectionTitle(text: String) =
    Text(text.uppercase(), color = Night.Muted, fontSize = 13.sp, letterSpacing = 0.2.sp)

@Composable
private fun TimeCell(label: String, time: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = if (highlight) Sky.Gold else Sky.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Sky.Muted, fontSize = 11.sp)
    }
}

@Composable
internal fun Chips(options: List<Pair<String, Int>>, selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            val on = value == selected
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (on) Night.Gold else Night.Fill)
                    .selectable(selected = on, onClick = { onPick(value) }).padding(horizontal = 14.dp, vertical = 7.dp),
            ) { Text(label, color = if (on) Night.OnAccent else Night.Text, fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
}

// ------------------------------------------------------------------------------------------------

@Composable
private fun StreakCard(prefs: Prefs, tonight: LocalDate, @Suppress("UNUSED_PARAMETER") version: Int) {
    val streak = prefs.streak(tonight)
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(G.FLAME, Night.Warn, 36.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (streak == 0) "Start your streak tonight" else "$streak night${if (streak == 1) "" else "s"} in a row",
                    color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                )
                Text("Best: ${prefs.best()} · Total: ${prefs.wokeNights.size}", color = Night.Muted, fontSize = 13.sp)
            }
        }
        // Last 14 nights
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 13 downTo 0) {
                val d = tonight.minusDays(i.toLong())
                val woke = prefs.woke(d)
                Box(
                    Modifier.size(16.dp).clip(CircleShape)
                        .background(if (woke) Rings.Deen else Night.Fill),
                )
            }
        }
    }
}

@Composable
private fun Checklist(ctx: Context, prefs: Prefs, @Suppress("UNUSED_PARAMETER") version: Int, onNotif: () -> Unit, onLocation: () -> Unit) {
    val pkg = ctx.packageName
    val nm = ctx.getSystemService(NotificationManager::class.java)
    val pm = ctx.getSystemService(PowerManager::class.java)

    data class Item(val title: String, val why: String, val ok: Boolean, val fix: () -> Unit)

    val items = listOf(
        Item(
            "Notifications", "So the alarm can appear on your lock screen",
            nm.areNotificationsEnabled(), onNotif,
        ),
        Item(
            "Exact alarms", "So Android rings on the minute, not \"roughly\"",
            Scheduler.canExact(ctx),
        ) {
            if (Build.VERSION.SDK_INT >= 31) ctx.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$pkg")),
            )
        },
        Item(
            "Full-screen alarm", "So the alarm page opens over the lock screen",
            Build.VERSION.SDK_INT < 34 || runCatching { nm.canUseFullScreenIntent() }.getOrDefault(true),
        ) {
            if (Build.VERSION.SDK_INT >= 34) ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$pkg")),
            )
        },
        Item(
            "Battery: unrestricted", "So your phone never puts the alarm to sleep",
            runCatching { pm.isIgnoringBatteryOptimizations(pkg) }.getOrDefault(true),
        ) {
            @SuppressLint("BatteryLife")
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$pkg"))
            runCatching { ctx.startActivity(i) }
        },
        Item(
            "Your location", "For accurate Maghrib and Fajr",
            prefs.hasLocation, onLocation,
        ),
        Item(
            "Telegram", "Optional: messages with buttons that work like the alarm",
            prefs.telegramReady,
        ) {},
    )
    val todo = items.filter { !it.ok }
    if (todo.isEmpty()) return

    Card {
        SectionTitle("Finish setting up · ${items.size - todo.size}/${items.size}")
        todo.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Night.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(item.why, color = Night.Muted, fontSize = 12.sp)
                }
                if (item.title != "Telegram") {
                    TextButton(onClick = { runCatching { item.fix() } }) { Text("Fix", color = Night.Gold) }
                } else {
                    Text("below ↓", color = Night.Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(prefs: Prefs, @Suppress("UNUSED_PARAMETER") version: Int, onChanged: () -> Unit) {
    var methodOpen by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf("") }
    Card {
        SectionTitle("Calculation")
        Box {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Night.CardEdge, RoundedCornerShape(12.dp))
                    .clickable { methodOpen = true }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(prefs.method.label, color = Night.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("▾", color = Night.Muted)
            }
            DropdownMenu(expanded = methodOpen, onDismissRequest = { methodOpen = false }) {
                Method.entries.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.label) },
                        onClick = { prefs.method = m; methodOpen = false; onChanged() },
                    )
                }
            }
        }
        OutlinedTextField(
            value = manual, onValueChange = { manual = it },
            label = { Text("Or set coordinates manually: lat, lng") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            colors = fieldColors(),
            trailingIcon = {
                TextButton(onClick = {
                    val parts = manual.split(",").map { it.trim().toDoubleOrNull() }
                    if (parts.size == 2 && parts[0] != null && parts[1] != null) {
                        prefs.lat = parts[0]!!; prefs.lng = parts[1]!!
                        prefs.placeName = String.format(Locale.US, "%.3f, %.3f", parts[0], parts[1])
                        manual = ""; onChanged()
                    }
                }) { Text("Set", color = Night.Gold) }
            },
        )

        SectionTitle("Wake me")
        Chips(listOf("At start" to 0, "+10m" to 10, "+20m" to 20, "+30m" to 30), prefs.offsetMinutes) {
            prefs.offsetMinutes = it; onChanged()
        }
        SectionTitle("Gentle heads-up before")
        Chips(listOf("Off" to 0, "10m" to 10, "15m" to 15, "30m" to 30), prefs.preAlarmMinutes) {
            prefs.preAlarmMinutes = it; onChanged()
        }
        SectionTitle("Snooze length")
        Chips(listOf("5m" to 5, "10m" to 10, "15m" to 15), prefs.snoozeMinutes) {
            prefs.snoozeMinutes = it; onChanged()
        }
    }
}

@Composable
private fun TelegramCard(prefs: Prefs, @Suppress("UNUSED_PARAMETER") version: Int, onChanged: () -> Unit, onToast: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(prefs.botToken) }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Card {
        SectionTitle("Telegram")
        if (prefs.telegramReady) {
            Text("Connected. Each night you'll get a message with \"I'm up\" and \"Snooze\" buttons that control the alarm.", color = Night.Text, fontSize = 14.sp)
        } else {
            Text(
                "1. In Telegram, message @BotFather, send /newbot, and copy the token.\n" +
                    "2. Paste it below and tap Connect.\n3. Open your bot and press Start.",
                color = Night.Muted, fontSize = 13.sp, lineHeight = 20.sp,
            )
        }
        OutlinedTextField(
            value = token, onValueChange = { token = it.trim() },
            label = { Text("Bot token") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        status?.let { Text(it, color = Night.Gold, fontSize = 13.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                enabled = !busy && token.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Night.Gold, contentColor = Night.OnAccent),
                onClick = {
                    busy = true
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { connectTelegram(ctx, prefs, token) { s -> status = s } }
                        busy = false; status = null; onToast(result); onChanged()
                    }
                },
            ) { Text(if (prefs.telegramReady) "Reconnect" else "Connect") }
            if (prefs.telegramReady) {
                TextButton(onClick = { prefs.botToken = ""; prefs.chatId = 0L; token = "" }) {
                    Text("Disconnect", color = Night.Dawn)
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                Telegram(prefs.botToken).send(prefs.chatId, "🌙 Qiyam is connected. See you in the last third, in sha Allah.")
                            }.isSuccess
                        }
                        onToast(if (ok) "Test message sent" else "Couldn't reach Telegram")
                    }
                }) { Text("Send test", color = Night.Gold) }
            }
        }
    }
}

/** Validates the token, asks you to press Start, then picks up your chat id. Runs on IO. */
private fun connectTelegram(ctx: Context, prefs: Prefs, token: String, progress: (String) -> Unit): String {
    val tg = Telegram(token)
    val bot = runCatching { tg.botName() }.getOrElse { return "That token didn't work: ${it.message}" }
    prefs.botToken = token
    // Open the bot so you can press Start.
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$bot?start=qiyam")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    progress("Waiting for you to press Start in @$bot…")
    val deadline = System.currentTimeMillis() + 120_000
    var offset = 0L
    while (System.currentTimeMillis() < deadline) {
        val ups = runCatching { tg.updates(offset, 20) }.getOrElse { Thread.sleep(3000); emptyList() }
        for (u in ups) {
            offset = u.updateId + 1
            val chat = u.chatId ?: continue
            if (u.text != null) {
                prefs.chatId = chat
                prefs.updateOffset = offset
                runCatching {
                    tg.send(
                        chat,
                        "🌙 <b>As-salamu alaykum!</b>\nQiyam is connected. I'll message you when the last third of the night begins, " +
                            "with buttons to stop or snooze your alarm from here.",
                    )
                }
                return "Telegram connected ✅"
            }
        }
    }
    return "Didn't see a Start from you. Open @$bot, press Start, then tap Connect again."
}

@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Night.Gold, unfocusedBorderColor = Night.CardEdge,
    focusedLabelColor = Night.Gold, unfocusedLabelColor = Night.Muted,
    focusedTextColor = Night.Text, unfocusedTextColor = Night.Text, cursorColor = Night.Gold,
)

// ------------------------------------------------------------------------------------------------

/** Gets a fresh location (network first, then GPS), names the place, and saves it. */
@SuppressLint("MissingPermission")
internal fun fetchLocation(ctx: Context, prefs: Prefs, done: (String) -> Unit) {
    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    val lm = ctx.getSystemService(LocationManager::class.java)
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter {
        runCatching { lm.isProviderEnabled(it) }.getOrDefault(false)
    }
    fun save(loc: Location?) {
        val l = loc ?: providers.firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        if (l == null) { done("Couldn't get a location. Turn on Location, or set coordinates manually."); return }
        prefs.lat = l.latitude; prefs.lng = l.longitude
        prefs.placeName = String.format(Locale.US, "%.3f, %.3f", l.latitude, l.longitude)
        done("Location updated")
        Thread {
            runCatching {
                @Suppress("DEPRECATION")
                val a = Geocoder(ctx).getFromLocation(l.latitude, l.longitude, 1)?.firstOrNull()
                val name = a?.locality ?: a?.subAdminArea ?: a?.adminArea
                if (name != null) prefs.placeName = name
            }
        }.start()
    }
    val provider = providers.firstOrNull() ?: return save(null)
    if (Build.VERSION.SDK_INT >= 30) {
        lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(ctx)) { save(it) }
    } else {
        save(null)
    }
}
