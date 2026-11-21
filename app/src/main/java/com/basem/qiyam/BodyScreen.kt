package com.basem.qiyam

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

    Page("Body", "$doneThisWeek of ${planned.size} sessions this week") {

        // This week
        Card {
            SectionTitle("This week")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { d ->
                    val done = log.done(d, "gym")
                    val plan = d.dayOfWeek.value in planned
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (done) Night.Gold else Color.Transparent)
                            .border(1.dp, if (plan || done) Night.Gold.copy(alpha = if (done) 1f else 0.5f) else Night.CardEdge, RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(d.dayOfWeek.name.take(1), color = if (done) Night.Ink else if (d == day) Night.Gold else Night.Muted, fontSize = 12.sp)
                        Text(
                            if (done) log.text(d, "gym").take(4).ifBlank { "✓" } else if (plan) "plan" else "rest",
                            color = if (done) Night.Ink else Night.Muted, fontSize = 10.sp, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Log today
        Card {
            SectionTitle("Today's session")
            val type = log.text(day, "gym")
            Text(
                if (log.done(day, "gym")) "Logged: $type 💪" else if (day.dayOfWeek.value in planned) "Planned gym day. What are you training?" else "Rest day. Log it if you train anyway.",
                color = Night.Text, fontSize = 14.sp,
            )
            GymTypes.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { t ->
                        StatusChip(t, log.done(day, "gym") && type == t, Night.Gold, modifier = Modifier.weight(1f), compact = true) {
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
                Sparkline(history.map { it.second }, Modifier.width(140.dp).height(48.dp))
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
                        dow.name.take(2).lowercase().replaceFirstChar { it.uppercase() }, on, Night.Gold,
                        modifier = Modifier.weight(1f), compact = true,
                    ) {
                        prefs.gymDays = if (on) planned - dow.value else planned + dow.value
                    }
                }
            }
            Text("Planned days count toward your daily score. Extra sessions earn a bonus.", color = Night.Muted, fontSize = 12.sp)
        }
        Box(Modifier.padding(top = 4.dp)) {
            Text("Coming later: steps, sleep and weight from Samsung Health.", color = Night.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}
