package com.basem.qiyam.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The night as an arc over the horizon: Maghrib on the left, Fajr on the right, split into
 * thirds. The last third glows gold; the moon marks where you are in the night now.
 *
 * @param progress 0 at Maghrib, 1 at Fajr, or null during daytime.
 */
@Composable
fun NightDial(
    progress: Float?,
    modifier: Modifier = Modifier,
    center: @Composable BoxScope.() -> Unit,
) {
    val stars = remember {
        val rnd = Random(7)
        List(70) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
    }
    val twinkle = if (Motion.live) {
        rememberInfiniteTransition(label = "twinkle").animateFloat(
            initialValue = 0.35f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
            label = "twinkle",
        ).value
    } else 0.8f
    val inLastThird = progress != null && progress >= 2f / 3f

    Box(modifier.fillMaxWidth().aspectRatio(1.55f), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val stroke = min(w, h) * 0.045f
            val r = min(w / 2f - stroke * 1.5f, h - stroke * 2.5f)
            val c = Offset(w / 2f, h - stroke * 1.2f)

            // Stars, brighter near the top of the sky.
            stars.forEachIndexed { i, (x, y, s) ->
                val alpha = (if (i % 3 == 0) twinkle else 0.8f) * (1f - y) * 0.9f
                drawCircle(Color.White.copy(alpha = alpha), radius = 0.8f + s * 1.8f, center = Offset(x * w, y * h * 0.9f))
            }

            val topLeft = Offset(c.x - r, c.y - r)
            val arcSize = Size(r * 2, r * 2)

            // First two thirds.
            drawArc(Sky.Edge, 180f, 119f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // The last third, glowing.
            if (inLastThird) drawArc(
                Sky.Gold.copy(alpha = 0.22f), 300f, 60f, false,
                topLeft.minus(Offset(stroke, stroke)), Size(arcSize.width + stroke * 2, arcSize.height + stroke * 2),
                style = Stroke(stroke * 2.2f, cap = StrokeCap.Round),
            )
            drawArc(
                Brush.sweepGradient(listOf(Sky.Gold, Sky.Dawn, Sky.Gold), c),
                300f, 60f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round),
            )

            // Ticks at Maghrib, each third, and Fajr.
            listOf(180f, 240f, 300f, 360f).forEach { deg ->
                val a = Math.toRadians(deg.toDouble())
                val inner = Offset(c.x + (r - stroke * 1.6f) * cos(a).toFloat(), c.y + (r - stroke * 1.6f) * sin(a).toFloat())
                val outer = Offset(c.x + (r - stroke * 0.9f) * cos(a).toFloat(), c.y + (r - stroke * 0.9f) * sin(a).toFloat())
                drawLine(Sky.Muted, inner, outer, strokeWidth = 3f, cap = StrokeCap.Round)
            }

            // Horizon.
            drawLine(
                Brush.horizontalGradient(listOf(Color.Transparent, Sky.Muted.copy(alpha = 0.5f), Color.Transparent)),
                Offset(0f, c.y + stroke * 0.8f), Offset(w, c.y + stroke * 0.8f), strokeWidth = 2f,
            )

            // The moon: where we are in the night.
            if (progress != null) {
                val a = Math.toRadians((180f + 180f * progress).toDouble())
                val m = Offset(c.x + r * cos(a).toFloat(), c.y + r * sin(a).toFloat())
                drawCircle(Sky.Gold.copy(alpha = 0.25f * twinkle + 0.1f), radius = stroke * 2.4f, center = m)
                drawCircle(Sky.Text, radius = stroke * 0.95f, center = m)
                drawCircle(Sky.Ink, radius = stroke * 0.8f, center = m.plus(Offset(stroke * 0.45f, -stroke * 0.25f)))
            }
        }
        center()
    }
}
