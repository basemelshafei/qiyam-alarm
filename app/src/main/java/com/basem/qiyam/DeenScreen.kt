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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Badge
import com.basem.qiyam.ui.G
import com.basem.qiyam.ui.Glyph
import com.basem.qiyam.ui.Night
import com.basem.qiyam.ui.Rings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun DeenScreen(onOpenQiyam: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val log = remember { DayLog(ctx) }
    val zone = prefs.zone
    val now = rememberNow()
    val day = now.atZone(zone).toLocalDate()
    val times = NightCalculator.prayers(prefs.lat, prefs.lng, prefs.method, day, zone)
    val fmt = DateTimeFormatter.ofPattern("HH:mm").withZone(zone)
    val items = Plan.items(prefs, log, day, zone)

    Page("Deen", "Today's score ${Plan.pillarScore(items, Pillar.DEEN)} · ${hijri(day)}") {

        // Prayers today, with full status
        Card {
            SectionTitle("Prayers today")
            Prayer.entries.forEach { p ->
                val status = log.prayer(day, p)
                Column(Modifier.testTag("prayer_${p.id}")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(p.label, color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(p.arabic, color = Night.Muted, fontSize = 15.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(fmt.format(p.time(times)), color = Night.Muted, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        listOf(PrayerStatus.ON_TIME, PrayerStatus.JAMAAH, PrayerStatus.LATE, PrayerStatus.MISSED).forEach { s ->
                            StatusChip(s.label, status == s, statusColor(s)) {
                                log.setPrayer(day, p, if (status == s) PrayerStatus.NONE else s)
                            }
                        }
                    }
                }
            }
        }

        // The week at a glance
        Card {
            SectionTitle("This week")
            val monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val week = (0L..6L).map { monday.plusDays(it) }
            Row {
                Spacer(Modifier.width(64.dp))
                week.forEach { d ->
                    Text(
                        d.dayOfWeek.name.take(1), color = if (d == day) Night.Gold else Night.Muted, fontSize = 12.sp,
                        modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            Prayer.entries.forEach { p ->
                WeekRow(p.label, week, day) { d -> statusColor(log.prayer(d, p)).takeIf { log.prayer(d, p) != PrayerStatus.NONE } }
            }
            WeekRow("Qiyam", week, day) { d -> Rings.Deen.takeIf { prefs.woke(d) || log.done(d, "qiyam") } }
            WeekRow("Adhkar", week, day) { d ->
                val n = listOf("adhkar_am", "adhkar_pm").count { log.done(d, it) }
                when (n) { 2 -> Rings.Deen; 1 -> Night.Warn; else -> null }
            }
            WeekRow("Quran", week, day) { d ->
                val n = listOf("quran_new", "quran_rev").count { log.done(d, it) }
                when (n) { 2 -> Rings.Deen; 1 -> Night.Warn; else -> null }
            }
        }

        // Fasting
        Card {
            SectionTitle("Monday & Thursday fasts")
            val nextFast = generateSequence(day) { it.plusDays(1) }.first { Plan.isFastDay(it) }
            val month = day.withDayOfMonth(1)
            val fastDays = generateSequence(month) { it.plusDays(1) }.takeWhile { it.month == day.month }.filter { Plan.isFastDay(it) }.toList()
            val kept = fastDays.count { log.done(it, "fast") }
            Text(
                if (Plan.isFastDay(day)) "Today is a fast day." else "Next: ${nextFast.format(DateTimeFormatter.ofPattern("EEEE d MMM"))}. Your Qiyam alarm will remind you about suhoor.",
                color = Night.Text, fontSize = 14.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                fastDays.forEach { d ->
                    val done = log.done(d, "fast")
                    val future = d.isAfter(day)
                    StatusChip(
                        d.format(DateTimeFormatter.ofPattern("d")), done,
                        Rings.Deen, enabled = !future, modifier = Modifier.weight(1f), compact = true,
                    ) { log.toggle(d, "fast") }
                }
            }
            Text("$kept of ${fastDays.count { !it.isAfter(day) }} so far this month", color = Night.Muted, fontSize = 12.sp)
        }

        // Adhkar
        Card {
            SectionTitle("Adhkar")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Morning", log.done(day, "adhkar_am"), Rings.Deen) { log.toggle(day, "adhkar_am") }
                StatusChip("Evening", log.done(day, "adhkar_pm"), Rings.Deen) { log.toggle(day, "adhkar_pm") }
            }
            Text("Morning after Fajr until sunrise (${fmt.format(times.sunrise)}); evening after Asr until Maghrib.", color = Night.Muted, fontSize = 12.sp)
        }

        // Qiyam
        Card {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onOpenQiyam),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Badge(G.MOON, Color(0xFF5E5CE6), 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val streak = prefs.streak(day)
                    Text("Qiyam alarm", color = Night.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "$streak-night streak · best ${prefs.best()}" +
                            if (prefs.enabled) " · alarm on" else " · alarm off",
                        color = Night.Muted, fontSize = 13.sp,
                    )
                }
                Glyph(G.CHEVRON, Night.Muted, 18.dp)
            }
        }

        // Quran
        Card {
            SectionTitle("Quran memorisation")
            var current by remember { mutableStateOf(prefs.quranCurrent) }
            OutlinedTextField(
                value = current, onValueChange = { current = it },
                label = { Text("Memorising now (e.g. Al-Mulk 1–10)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
                trailingIcon = {
                    if (current.trim() != prefs.quranCurrent) {
                        TextButton(onClick = { prefs.quranCurrent = current }) { Text("Save", color = Night.Gold) }
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Memorised today", log.done(day, "quran_new"), Rings.Deen) { log.toggle(day, "quran_new") }
                StatusChip("Revised today", log.done(day, "quran_rev"), Rings.Deen) { log.toggle(day, "quran_rev") }
            }
            val last30 = (0L..29L).map { day.minusDays(it) }
            Text(
                "Last 30 days: memorised on ${last30.count { log.done(it, "quran_new") }}, revised on ${last30.count { log.done(it, "quran_rev") }}",
                color = Night.Muted, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun WeekRow(label: String, week: List<LocalDate>, today: LocalDate, color: (LocalDate) -> Color?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Night.Text, fontSize = 13.sp, modifier = Modifier.width(64.dp))
        week.forEach { d ->
            val c = color(d)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(14.dp).clip(CircleShape)
                        .background(c ?: if (d.isAfter(today)) Color.Transparent else Night.CardEdge)
                        .border(1.5.dp, if (d == today) Night.Gold else Color.Transparent, CircleShape),
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    label: String, on: Boolean, color: Color, enabled: Boolean = true,
    modifier: Modifier = Modifier, compact: Boolean = false, onClick: () -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(50))
            .background(if (on) color else Night.Fill.copy(alpha = if (enabled) Night.Fill.alpha else Night.Fill.alpha / 2))
            .selectable(selected = on, enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = if (compact) 2.dp else 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, maxLines = 1, softWrap = false,
            color = if (on) Night.OnAccent else if (enabled) Night.Text else Night.Muted,
            fontSize = if (compact) 13.sp else 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

fun statusColor(s: PrayerStatus): Color = when (s) {
    PrayerStatus.JAMAAH -> Rings.Deen
    PrayerStatus.ON_TIME -> Night.Gold
    PrayerStatus.LATE -> Night.Warn
    PrayerStatus.MISSED -> Night.Dawn
    PrayerStatus.NONE -> Night.CardEdge
}
