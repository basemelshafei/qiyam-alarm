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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Night
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

    Page(
        title = day.format(DateTimeFormatter.ofPattern("EEEE d MMMM")),
        subtitle = listOf(hijri(day), "📍 ${prefs.placeName}").filter { it.isNotBlank() }.joinToString(" · "),
    ) {
        // Score + pillars
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreRing(score, 108.dp, label = "today")
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(Pillar.DEEN to Tab.DEEN, Pillar.BODY to Tab.BODY).forEach { (p, t) ->
                        PillarBar(p, Plan.pillarScore(items, p)) { onOpen(t) }
                    }
                    Text(
                        "${items.count { it.progress >= 1f }} of ${items.count { it.weight > 0 }} done",
                        color = Night.Muted, fontSize = 12.sp,
                    )
                }
            }
        }

        // Next up
        val next = items.firstOrNull { it.progress < 1f && it.weight > 0 && !it.time.isBefore(now.minusSeconds(3 * 3600)) }
        if (next != null) {
            Card {
                SectionTitle("Next up")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(next.pillar.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(next.title, color = Night.Text, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (next.time.isAfter(now)) "at ${fmt.format(next.time)} · in ${human(java.time.Duration.between(now, next.time))}"
                            else "since ${fmt.format(next.time)}",
                            color = Night.Muted, fontSize = 13.sp,
                        )
                    }
                }
            }
        }

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
                        ItemKind.GYM -> if (log.done(day, "gym")) {
                            log.setValue(day, "gym", 0); log.setText(day, "gym", "")
                        } else gymPicker = true
                    }
                }
            }
            if (!nowShown) NowMarker(fmt.format(now))
            Text(
                "Tap to tick off. For Jama'ah, late or missed prayers, use the Deen tab.",
                color = Night.Muted, fontSize = 11.sp,
            )
        }

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

    if (gymPicker) {
        GymTypeDialog(onPick = { type ->
            log.setText(day, "gym", type); log.setValue(day, "gym", 1); gymPicker = false
        }, onDismiss = { gymPicker = false })
    }
}

@Composable
private fun PillarBar(p: Pillar, score: Int, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row {
            Text("${p.emoji} ${p.label}", color = Night.Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("$score", color = scoreColor(score), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Night.CardEdge)) {
            Box(Modifier.fillMaxWidth(score / 100f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(scoreColor(score)))
        }
    }
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
        Text(time, color = if (past && !done) Night.Muted else Night.Text, fontSize = 13.sp, modifier = Modifier.width(48.dp))
        Text(item.pillar.emoji, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = if (done) Night.Muted else Night.Text,
                fontSize = 16.sp, fontWeight = FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
            )
            Text(item.note, color = Night.Muted, fontSize = 12.sp, maxLines = 1)
        }
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (done) Night.Gold else if (partial) Night.Warn.copy(alpha = 0.5f) else Night.Ink)
                .border(2.dp, if (done) Night.Gold else Night.CardEdge, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) Text("✓", color = Night.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
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
                                Modifier.clip(RoundedCornerShape(50)).border(1.dp, Night.CardEdge, RoundedCornerShape(50))
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
            Text(tab.icon, fontSize = 36.sp)
            features.forEach { Text("•  $it", color = Night.Text, fontSize = 15.sp) }
        }
    }
}
