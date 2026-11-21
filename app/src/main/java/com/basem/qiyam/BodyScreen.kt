package com.basem.qiyam

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.coerceAtLeast
import androidx.health.connect.client.PermissionController
import com.basem.qiyam.ui.Badge
import com.basem.qiyam.ui.G
import com.basem.qiyam.ui.Glyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.Rings
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun BodyScreen() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val log = remember { DayLog(ctx) }
    val zone = prefs.zone
    val day = rememberNow().atZone(zone).toLocalDate()
    val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val week = (0L..6L).map { monday.plusDays(it) }
    val planned = prefs.gymDays
    val doneThisWeek = week.count { log.done(it, "gym") }

    val workouts = remember(prefs.lyftaSyncedAt) { Lyfta.workouts(ctx) }
    val health = log.health(day)
    val doneWeek = week.count { log.done(it, "gym") || Plan.gymSource(log, it) != null }

    Page("Body", "$doneWeek of ${planned.size} sessions this week") {

        // Today from your Fitbit (via Health Connect)
        if (prefs.healthSyncedAt > 0L) {
            Card {
                SectionTitle("Today")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val goal = prefs.stepGoal.coerceAtLeast(1)
                    ActivityRings(
                        listOf(
                            RingValue((health.steps ?: 0L).toFloat() / goal, Rings.Body),
                            RingValue((health.sleepMinutes ?: 0L) / 420f, Color(0xFF5E5CE6)),
                        ),
                        size = 96.dp, stroke = 12.dp,
                    )
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric(G.STEPS, Rings.Body, "Steps", String.format(Locale.US, "%,d", health.steps ?: 0), "/ ${String.format(Locale.US, "%,d", goal)}")
                        Metric(G.BED, Color(0xFF5E5CE6), "Sleep", health.sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", "")
                        Metric(G.HEART, Night.Dawn, "Resting", health.restingHr?.toString() ?: "—", "bpm")
                    }
                }
                val last7 = (0L..6L).map { day.minusDays(it) }.mapNotNull { log.health(it).steps }
                if (last7.isNotEmpty()) {
                    Text("7-day average: ${String.format(Locale.US, "%,d", last7.sum() / last7.size)} steps", color = Night.Muted, fontSize = 13.sp)
                }
            }
        }

        // This week
        Card {
            SectionTitle("This week")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    val done = log.done(d, "gym") || Plan.gymSource(log, d) != null
                    val plan = d.dayOfWeek.value in planned
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (done) Rings.Body else if (plan) Rings.Body.copy(alpha = 0.12f) else Night.Fill)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d.dayOfWeek.name.take(1), color = if (done) Night.OnAccent else if (d == day) Night.Gold else Night.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (done) log.text(d, "gym").ifBlank { log.text(d, "lyfta") }.take(4).ifBlank { "done" } else if (plan) "plan" else "rest",
                            color = if (done) Night.OnAccent else Night.Muted, fontSize = 10.sp, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Workouts from Lyfta
        if (workouts.isNotEmpty()) {
            Card {
                SectionTitle("Workouts · Lyfta")
                WeeklyVolume(workouts, day)
                workouts.take(6).forEachIndexed { i, w ->
                    if (i > 0) HorizontalDivider(color = Night.CardEdge, thickness = 0.5.dp)
                    WorkoutRow(w)
                }
            }
        }

        // Log today
        Card {
            SectionTitle("Today's session")
            val type = log.text(day, "gym")
            Text(
                if (log.done(day, "gym")) "Logged: $type" else if (day.dayOfWeek.value in planned) "Planned gym day. What are you training?" else "Rest day. Log it if you train anyway.",
                color = Night.Text, fontSize = 14.sp,
            )
            GymTypes.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { t ->
                        StatusChip(t, log.done(day, "gym") && type == t, Rings.Body, modifier = Modifier.weight(1f), compact = true) {
                            if (log.done(day, "gym") && type == t) {
                                log.setValue(day, "gym", 0); log.setText(day, "gym", "")
                            } else {
                                log.setText(day, "gym", t); log.setValue(day, "gym", 1)
                            }
                        }
                    }
                }
            }
            val last28 = (0L..27L).map { day.minusDays(it) }
            Text("Last 4 weeks: ${last28.count { log.done(it, "gym") }} sessions", color = Night.Muted, fontSize = 12.sp)
        }

        // Bodyweight
        Card {
            SectionTitle("Bodyweight")
            val history = log.weights(day, 90)
            var input by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val latest = history.lastOrNull()
                    Text(
                        latest?.let { String.format(Locale.US, "%.1f kg", it.second) } ?: "No entries yet",
                        color = Night.Text, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                    )
                    val monthAgo = history.firstOrNull { !it.first.isBefore(day.minusDays(30)) }
                    if (latest != null && monthAgo != null && monthAgo != latest) {
                        val diff = latest.second - monthAgo.second
                        Text(
                            String.format(Locale.US, "%+.1f kg since %s", diff, monthAgo.first.format(DateTimeFormatter.ofPattern("d MMM"))),
                            color = Night.Muted, fontSize = 12.sp,
                        )
                    }
                }
                Sparkline(history.map { it.second }, Modifier.width(140.dp).height(48.dp), color = Rings.Body)
            }
            OutlinedTextField(
                value = input, onValueChange = { input = it.replace(',', '.') },
                label = { Text("Today's weight (kg)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                trailingIcon = {
                    TextButton(onClick = {
                        input.toFloatOrNull()?.takeIf { it in 30f..300f }?.let { log.setWeight(day, it); input = "" }
                    }) { Text("Save", color = Night.Gold) }
                },
            )
        }

        // Plan
        Card {
            SectionTitle("Gym days")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DayOfWeek.entries.forEach { dow ->
                    val on = dow.value in planned
                    StatusChip(
                        dow.name.take(2).lowercase().replaceFirstChar { it.uppercase() }, on, Rings.Body,
                        modifier = Modifier.weight(1f), compact = true,
                    ) {
                        prefs.gymDays = if (on) planned - dow.value else planned + dow.value
                    }
                }
            }
            Text("Planned days count toward your daily score. Extra sessions earn a bonus.", color = Night.Muted, fontSize = 12.sp)
        }
        ConnectedApps(prefs)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Metric(g: G, color: Color, label: String, value: String, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Glyph(g, color, 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = Night.Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        if (unit.isNotBlank()) Text(" $unit", color = Night.Muted, fontSize = 13.sp)
    }
}

/** Total lifted per week for the last 8 weeks, like a Fitness trend. */
@Composable
private fun WeeklyVolume(workouts: List<LyftaWorkout>, today: java.time.LocalDate) {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weeks = (7 downTo 0).map { monday.minusWeeks(it.toLong()) }
    val vols = weeks.map { w -> workouts.filter { !it.date.isBefore(w) && it.date.isBefore(w.plusWeeks(1)) }.sumOf { it.volume } }
    val max = vols.maxOrNull()?.takeIf { it > 0 } ?: return
    val unit = workouts.first().unit
    Text(
        "This week: ${String.format(Locale.US, "%,.0f", vols.last())} $unit lifted",
        color = Night.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
    )
    Row(Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        vols.forEachIndexed { i, v ->
            Box(
                Modifier.weight(1f).height((64 * (v / max)).toFloat().dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(4.dp)).background(if (i == vols.lastIndex) Rings.Body else Rings.Body.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun WorkoutRow(w: LyftaWorkout) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(G.DUMBBELL, Rings.Body, 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(w.title, color = Night.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    listOf(
                        w.date.format(DateTimeFormatter.ofPattern("EEE d MMM")),
                        "${w.exercises.size} exercises",
                        "${w.setCount} sets",
                        w.duration.removePrefix("00:").ifBlank { null },
                    ).filterNotNull().joinToString(" · "),
                    color = Night.Muted, fontSize = 13.sp,
                )
            }
            if (w.records > 0) {
                Text("${w.records} PR", color = Night.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
            }
            Glyph(G.CHEVRON, Night.Muted, 16.dp, Modifier.graphicsLayer { rotationZ = if (open) 90f else 0f })
        }
        if (open) {
            Column(Modifier.padding(start = 44.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                w.exercises.forEach { e ->
                    Row {
                        Text(e.name, color = Night.Text, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        val done = e.sets.filter { it.completed }
                        Text(
                            buildString {
                                append("${done.size} × ")
                                append(done.mapNotNull { it.reps }.distinct().joinToString("/").ifBlank { "—" })
                                e.topSet?.let { append(" · ${fmtWeight(it.weight)} ${w.unit}") }
                            },
                            color = if (e.hasRecord) Night.Warn else Night.Muted, fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Fitbit (through Health Connect) and Lyfta: status, connect, sync. */
@Composable
private fun ConnectedApps(prefs: Prefs) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<Health.Status?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(refresh) { status = Health.status(ctx) }

    val askHealth = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        scope.launch {
            status = Health.status(ctx)
            if (Health.sync(ctx)) message = "Fitbit data synced"
        }
    }

    Card {
        SectionTitle("Connected apps")

        // Fitbit via Health Connect
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(G.HEART, Rings.Body, 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Fitbit (Health Connect)", color = Night.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (status) {
                        Health.Status.CONNECTED -> "Connected" + syncedAgo(prefs.healthSyncedAt)
                        Health.Status.NEEDS_PERMISSION -> "Allow access to steps, sleep, heart rate and workouts"
                        Health.Status.NOT_INSTALLED -> "Health Connect isn't on this phone yet"
                        null -> "Checking…"
                    },
                    color = Night.Muted, fontSize = 13.sp,
                )
            }
            TextButton(onClick = {
                when (status) {
                    Health.Status.NEEDS_PERMISSION -> askHealth.launch(Health.permissions)
                    Health.Status.NOT_INSTALLED -> runCatching {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                    else -> scope.launch { busy = true; Health.sync(ctx); busy = false; message = "Fitbit data synced"; refresh++ }
                }
            }) {
                Text(
                    when (status) { Health.Status.NEEDS_PERMISSION -> "Connect"; Health.Status.NOT_INSTALLED -> "Install"; else -> "Sync" },
                    color = Night.Gold, fontSize = 16.sp,
                )
            }
        }
        if (status != Health.Status.CONNECTED) {
            Text(
                "In the Google Health app, turn on sharing with Health Connect (Settings › Health Connect) so your Fitbit Air data reaches Istiqamah.",
                color = Night.Muted, fontSize = 13.sp,
            )
        }

        HorizontalDivider(color = Night.CardEdge, thickness = 0.5.dp)

        // Lyfta
        var key by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge(G.DUMBBELL, Color(0xFF1C1C1E), 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Lyfta", color = Night.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (prefs.lyftaKey.isBlank()) "Workouts with sets, reps and weights" else "Connected" + syncedAgo(prefs.lyftaSyncedAt),
                    color = Night.Muted, fontSize = 13.sp,
                )
            }
            if (prefs.lyftaKey.isNotBlank()) {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        message = withContext(Dispatchers.IO) { runCatching { Lyfta.sync(ctx) }.getOrElse { "Couldn't reach Lyfta: ${it.message}" } }
                        busy = false
                    }
                }) { Text("Sync", color = Night.Gold, fontSize = 16.sp) }
            }
        }
        if (prefs.lyftaKey.isBlank()) {
            Text(
                "On my.lyfta.app, open Settings › API and generate a key. Paste it here. It stays on this phone.",
                color = Night.Muted, fontSize = 13.sp,
            )
            OutlinedTextField(
                value = key, onValueChange = { key = it.trim() },
                label = { Text("Lyfta API key") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                trailingIcon = {
                    TextButton(enabled = key.isNotBlank() && !busy, onClick = {
                        prefs.lyftaKey = key
                        scope.launch {
                            busy = true
                            message = withContext(Dispatchers.IO) { runCatching { Lyfta.sync(ctx) }.getOrElse { "Couldn't reach Lyfta: ${it.message}" } }
                            if (message?.startsWith("Lyfta didn't accept") == true) prefs.lyftaKey = ""
                            busy = false; key = ""
                        }
                    }) { Text("Connect", color = Night.Gold) }
                },
            )
        }
        message?.let { Text(it, color = Night.Gold, fontSize = 13.sp) }
    }
}

private fun syncedAgo(at: Long): String {
    if (at <= 0L) return ""
    val mins = (System.currentTimeMillis() - at) / 60_000
    return when {
        mins < 1 -> " · synced just now"
        mins < 60 -> " · synced ${mins}m ago"
        mins < 24 * 60 -> " · synced ${mins / 60}h ago"
        else -> " · synced ${mins / (24 * 60)}d ago"
    }
}
