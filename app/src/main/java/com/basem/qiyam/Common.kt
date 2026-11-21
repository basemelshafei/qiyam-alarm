package com.basem.qiyam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.basem.qiyam.ui.Motion
import com.basem.qiyam.ui.Night
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** A scrolling page with a title, capped in width so it reads well on the unfolded screen. */
@Composable
fun Page(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.padding(top = 8.dp)) {
                // iOS "large title"
                Text(title, color = Night.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, lineHeight = 40.sp)
                if (subtitle != null) Text(subtitle, color = Night.Muted, fontSize = 15.sp)
            }
            content()
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** The current moment, ticking every second while motion is on. */
@Composable
fun rememberNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) { while (Motion.live) { delay(1000); now = Instant.now() } }
    return now
}

/** One ring of an Apple Fitness-style ring stack. */
data class RingValue(val progress: Float, val color: Color)

/**
 * Concentric activity rings, outermost first, with an optional label in the middle.
 * Each ring sits on a faint track of its own colour, like Apple's Fitness rings.
 */
@Composable
fun ActivityRings(
    rings: List<RingValue>, size: Dp, stroke: Dp = 14.dp, gap: Dp = 3.dp,
    center: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx(); val g = gap.toPx()
            rings.forEachIndexed { i, r ->
                val inset = sw / 2 + i * (sw + g)
                val tl = Offset(inset, inset)
                val box = Size(this.size.width - 2 * inset, this.size.height - 2 * inset)
                if (box.width <= 0f) return@forEachIndexed
                drawArc(r.color.copy(alpha = 0.22f), 0f, 360f, false, tl, box, style = Stroke(sw))
                val sweep = 360f * r.progress.coerceIn(0f, 1f)
                if (sweep > 0f) drawArc(r.color, -90f, sweep.coerceAtLeast(1f), false, tl, box, style = Stroke(sw, cap = StrokeCap.Round))
            }
        }
        center?.invoke()
    }
}

/** A single ring with the score written inside. */
@Composable
fun ScoreRing(score: Int, size: Dp, stroke: Dp = 10.dp, label: String? = null, color: Color = scoreColor(score)) {
    ActivityRings(listOf(RingValue(score / 100f, color)), size, stroke) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", color = Night.Text, fontSize = (size.value / 3.2f).sp, fontWeight = FontWeight.Bold)
            if (label != null) Text(label, color = Night.Muted, fontSize = 11.sp)
        }
    }
}

fun scoreColor(score: Int): Color = when {
    score >= 85 -> Night.Good
    score >= 60 -> Night.Gold
    score >= 35 -> Night.Warn
    else -> Night.Dawn
}

/** A small line chart of values over time. */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier, color: Color = Night.Gold) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.min(); val max = values.max()
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun pt(i: Int) = Offset(i * stepX, size.height - (values[i] - min) / span * size.height * 0.85f - size.height * 0.075f)
        for (i in 1 until values.size) drawLine(color, pt(i - 1), pt(i), strokeWidth = 4f, cap = StrokeCap.Round)
        drawCircle(color, 7f, pt(values.size - 1))
    }
}

private val hijriMonths = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
    "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah",
)

/** Hijri date (Umm al-Qura calendar; local moon-sighting can differ by a day). */
fun hijri(day: LocalDate): String = runCatching {
    val h = HijrahDate.from(day)
    "${h.get(ChronoField.DAY_OF_MONTH)} ${hijriMonths[h.get(ChronoField.MONTH_OF_YEAR) - 1]} ${h.get(ChronoField.YEAR)}"
}.getOrDefault("")
