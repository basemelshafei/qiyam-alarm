package com.basem.qiyam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Badge
import com.basem.qiyam.ui.G
import com.basem.qiyam.ui.Glyph
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.Rings
import java.time.format.DateTimeFormatter

/** The whole day as one timeline around the prayers, with a score that adds it all up. */
@Composable
fun TodayScreen(onOpen: (Tab) -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val log = remember { DayLog(ctx) }
    val zone = prefs.zone
    val now = rememberNow()
    val day = now.atZone(zone).toLocalDate()
    val items = Plan.items(prefs, log, day, zone)
    val score = Plan.score(items)
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    var gymPicker by remember { mutableStateOf(false) }

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val scoreCard: @Composable () -> Unit = {
        // Activity rings: one per area, the day's score in the middle
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val deen = Plan.pillarScore(items, Pillar.DEEN)
                val body = Plan.pillarScore(items, Pillar.BODY)
                ActivityRings(
                    listOf(RingValue(deen / 100f, Rings.Deen), RingValue(body / 100f, Rings.Body)),
                    size = 128.dp, stroke = 15.dp,
                ) {
                    Text("$score", color = Night.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RingLegend("Deen", deen, Rings.Deen) { onOpen(Tab.DEEN) }
                    RingLegend("Body", body, Rings.Body) { onOpen(Tab.BODY) }
                    Text(
                        "${items.count { it.progress >= 1f }} of ${items.count { it.weight > 0 }} done today",
                        color = Night.Muted, fontSize = 13.sp,
                    )
                }
            }
        }
    }
    val nextUp: @Composable () -> Unit = {
        // Next up
        val next = items.firstOrNull { it.progress < 1f && it.weight > 0 && !it.time.isBefore(now.minusSeconds(3 * 3600)) }
        if (next != null) {
            Card {
                SectionTitle("Next up")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge(glyphFor(next), pillarColor(next.pillar), 36.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(next.title, color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (next.time.isAfter(now)) "at ${fmt.format(next.time)} · in ${human(java.time.Duration.between(now, next.time))}"
                            else "since ${fmt.format(next.time)}",
                            color = Night.Muted, fontSize = 13.sp,
                        )
                    }
                }
            }
        }
    }
    val timeline: @Composable () -> Unit = {
        // Timeline
        Card {
            SectionTitle("Your day")
            var nowShown = false
            items.forEach { item ->
                if (!nowShown && item.time.isAfter(now)) {
                    NowMarker(fmt.format(now)); nowShown = true
                }
                TimelineRow(item, fmt.format(item.time), past = item.time.isBefore(now)) {
                    when (item.kind) {
                        ItemKind.TOGGLE, ItemKind.QIYAM -> log.toggle(day, item.id)
                        ItemKind.PRAYER -> {
                            val p = item.prayer!!
                            log.setPrayer(day, p, if (log.prayer(day, p) == PrayerStatus.NONE) PrayerStatus.ON_TIME else PrayerStatus.NONE)
                        }
                        ItemKind.GYM -> when {
                            log.done(day, "gym") -> { log.setValue(day, "gym", 0); log.setText(day, "gym", "") }
                            Plan.gymSource(log, day) != null -> onOpen(Tab.BODY)
                            else -> gymPicker = true
                        }
                        ItemKind.AUTO -> onOpen(Tab.BODY)
                    }
                }
            }
            if (!nowShown) NowMarker(fmt.format(now))
            Text(
                "Tap to tick off. For Jama'ah, late or missed prayers, use the Deen tab.",
                color = Night.Muted, fontSize = 11.sp,
            )
        }
    }
    val history: @Composable () -> Unit = {
        // Last 14 days
        Card {
            SectionTitle("Last 14 days")
            Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
                for (i in 13 downTo 0) {
                    val d = day.minusDays(i.toLong())
                    val s = Plan.scoreFor(prefs, log, d, zone)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth().height((70 * s / 100f).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(4.dp)).background(if (s == 0) Night.CardEdge else scoreColor(s)),
                        )
                        Text(d.dayOfWeek.name.take(1), color = if (i == 0) Night.Gold else Night.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
    }

    Page(
        title = "Today",
        overline = day.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
        subtitle = listOf(hijri(day), prefs.placeName).filter { it.isNotBlank() }.joinToString(" · "),
    ) {
        if (wide) {
            // Unfolded: the timeline on the left, the overview on the right.
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1.15f)) { timeline() }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    scoreCard(); nextUp(); history()
                }
            }
        } else {
            scoreCard(); nextUp(); timeline(); history()
        }
    }

    if (gymPicker) {
        GymTypeDialog(onPick = { type ->
            log.setText(day, "gym", type); log.setValue(day, "gym", 1); gymPicker = false
        }, onDismiss = { gymPicker = false })
    }
}

@Composable
private fun RingLegend(label: String, score: Int, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Text(label, color = Night.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$score", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("/100", color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

fun pillarColor(p: Pillar) = when (p) {
    Pillar.DEEN -> Rings.Deen; Pillar.BODY -> Rings.Body; Pillar.CAREER -> Rings.Career; Pillar.WEALTH -> Rings.Wealth
}

fun glyphFor(item: TodayItem): G = when {
    item.kind == ItemKind.QIYAM -> G.MOON
    item.kind == ItemKind.PRAYER -> G.DOME
    item.kind == ItemKind.GYM -> G.DUMBBELL
    item.id == "steps" -> G.STEPS
    item.id == "sleep" -> G.BED
    item.id == "fast" -> G.SUNRISE
    item.id.startsWith("adhkar") -> G.SPARKLE
    item.id.startsWith("quran") -> G.BOOK
    item.pillar == Pillar.CAREER -> G.BRIEFCASE
    item.pillar == Pillar.WEALTH -> G.CHART
    else -> G.CHECK
}

@Composable
private fun NowMarker(time: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(time, color = Night.Dawn, fontSize = 12.sp, modifier = Modifier.width(48.dp))
        Box(Modifier.size(8.dp).clip(CircleShape).background(Night.Dawn))
        Box(Modifier.weight(1f).height(1.dp).background(Night.Dawn.copy(alpha = 0.6f)))
        Text(" now", color = Night.Dawn, fontSize = 11.sp)
    }
}

@Composable
private fun TimelineRow(item: TodayItem, time: String, past: Boolean, onTap: () -> Unit) {
    val done = item.progress >= 1f
    val partial = item.progress > 0f && !done
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onTap() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = Night.Muted, fontSize = 13.sp, modifier = Modifier.width(46.dp))
        Badge(glyphFor(item), pillarColor(item.pillar), 28.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = if (done) Night.Muted else Night.Text,
                fontSize = 17.sp,
            )
            Text(item.note, color = Night.Muted, fontSize = 13.sp, maxLines = 1)
        }
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(if (done) Night.Gold else if (partial) Night.Warn else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.5.dp, if (done) Night.Gold else if (partial) Night.Warn else Night.Muted.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done || partial) Glyph(G.CHECK, Night.OnAccent, 16.dp) }
    }
}

@Composable
fun GymTypeDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Night.Card,
        title = { Text("What did you train?", color = Night.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GymTypes.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            Box(
                                Modifier.clip(RoundedCornerShape(50)).background(Night.Fill)
                                    .clickable { onPick(t) }.padding(horizontal = 14.dp, vertical = 8.dp),
                            ) { Text(t, color = Night.Text, fontSize = 14.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Night.Muted) } },
    )
}

/** Placeholder for the pillars arriving in stage 2. */
@Composable
fun ComingSoon(tab: Tab, features: List<String>) {
    Page(tab.label, "Coming in stage 2") {
        Card {
            Badge(tab.glyph, tab.color, 44.dp)
            features.forEach { Text("•  $it", color = Night.Text, fontSize = 15.sp) }
        }
    }
}
