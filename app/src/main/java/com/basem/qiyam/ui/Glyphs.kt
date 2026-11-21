package com.basem.qiyam.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Simple line icons in the spirit of Apple's SF Symbols, drawn in code. */
enum class G { SUN, MOON, DOME, DUMBBELL, BRIEFCASE, CHART, BOOK, SPARKLE, SUNRISE, FLAME, HEART, STEPS, BED, CHECK, CHEVRON, BELL, PIN, PAPERPLANE }

@Composable
fun Glyph(g: G, color: Color, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { draw(g, color) }
}

/** A white glyph on a coloured rounded square, like the icons in iPhone Settings. */
@Composable
fun Badge(g: G, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)).background(color),
        contentAlignment = Alignment.Center,
    ) { Glyph(g, Color.White, size * 0.62f) }
}

private fun DrawScope.draw(g: G, c: Color) {
    val w = size.width
    val s = w / 24f // design on a 24-unit grid
    val line = Stroke(width = 1.9f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun o(x: Float, y: Float) = Offset(x * s, y * s)
    when (g) {
        G.SUN -> {
            drawCircle(c, 4.6f * s, o(12f, 12f), style = line)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                drawLine(c, o(12f + 7.6f * cos(a).toFloat(), 12f + 7.6f * sin(a).toFloat()), o(12f + 10f * cos(a).toFloat(), 12f + 10f * sin(a).toFloat()), line.width, StrokeCap.Round)
            }
        }
        G.MOON -> {
            val outer = Path().apply { addOval(Rect(o(3f, 3f), Size(18f * s, 18f * s))) }
            val inner = Path().apply { addOval(Rect(o(8.5f, 0.5f), Size(16f * s, 16f * s))) }
            drawPath(Path().apply { op(outer, inner, PathOperation.Difference) }, c)
        }
        G.DOME -> {
            val p = Path().apply {
                moveTo(5f * s, 20f * s); lineTo(5f * s, 13f * s)
                cubicTo(5f * s, 8.5f * s, 12f * s, 7.5f * s, 12f * s, 4f * s)
                cubicTo(12f * s, 7.5f * s, 19f * s, 8.5f * s, 19f * s, 13f * s)
                lineTo(19f * s, 20f * s)
            }
            drawPath(p, c, style = line)
            drawLine(c, o(3f, 20f), o(21f, 20f), line.width, StrokeCap.Round)
            drawLine(c, o(12f, 2f), o(12f, 4f), line.width, StrokeCap.Round)
            drawRoundRect(c, o(10.2f, 15f), Size(3.6f * s, 5f * s), CornerRadius(1.8f * s), style = line)
        }
        G.DUMBBELL -> {
            drawLine(c, o(6f, 12f), o(18f, 12f), line.width, StrokeCap.Round)
            drawRoundRect(c, o(3.5f, 7f), Size(3f * s, 10f * s), CornerRadius(1.2f * s))
            drawRoundRect(c, o(17.5f, 7f), Size(3f * s, 10f * s), CornerRadius(1.2f * s))
            drawLine(c, o(1.8f, 10f), o(1.8f, 14f), line.width, StrokeCap.Round)
            drawLine(c, o(22.2f, 10f), o(22.2f, 14f), line.width, StrokeCap.Round)
        }
        G.BRIEFCASE -> {
            drawRoundRect(c, o(3f, 7.5f), Size(18f * s, 12.5f * s), CornerRadius(2.5f * s), style = line)
            drawRoundRect(c, o(8.5f, 3.8f), Size(7f * s, 3.7f * s), CornerRadius(1.4f * s), style = line)
            drawLine(c, o(3f, 12.5f), o(21f, 12.5f), line.width, StrokeCap.Round)
        }
        G.CHART -> {
            drawLine(c, o(6f, 20f), o(6f, 14f), line.width * 1.5f, StrokeCap.Round)
            drawLine(c, o(12f, 20f), o(12f, 9f), line.width * 1.5f, StrokeCap.Round)
            drawLine(c, o(18f, 20f), o(18f, 4f), line.width * 1.5f, StrokeCap.Round)
        }
        G.BOOK -> {
            val p = Path().apply {
                moveTo(12f * s, 6.5f * s); cubicTo(9f * s, 4.5f * s, 5.5f * s, 4.5f * s, 2.5f * s, 5.5f * s)
                lineTo(2.5f * s, 19f * s); cubicTo(5.5f * s, 18f * s, 9f * s, 18f * s, 12f * s, 20f * s)
                cubicTo(15f * s, 18f * s, 18.5f * s, 18f * s, 21.5f * s, 19f * s)
                lineTo(21.5f * s, 5.5f * s); cubicTo(18.5f * s, 4.5f * s, 15f * s, 4.5f * s, 12f * s, 6.5f * s)
                lineTo(12f * s, 20f * s)
            }
            drawPath(p, c, style = line)
        }
        G.SPARKLE -> {
            val p = Path().apply {
                moveTo(12f * s, 2.5f * s)
                quadraticBezierTo(13f * s, 11f * s, 21.5f * s, 12f * s)
                quadraticBezierTo(13f * s, 13f * s, 12f * s, 21.5f * s)
                quadraticBezierTo(11f * s, 13f * s, 2.5f * s, 12f * s)
                quadraticBezierTo(11f * s, 11f * s, 12f * s, 2.5f * s); close()
            }
            drawPath(p, c)
        }
        G.SUNRISE -> {
            drawArc(c, 180f, 180f, false, o(6.5f, 10f), Size(11f * s, 11f * s), style = line)
            drawLine(c, o(2.5f, 15.5f), o(21.5f, 15.5f), line.width, StrokeCap.Round)
            drawLine(c, o(12f, 3f), o(12f, 6.5f), line.width, StrokeCap.Round)
            drawLine(c, o(4.5f, 8f), o(6.5f, 10f), line.width, StrokeCap.Round)
            drawLine(c, o(19.5f, 8f), o(17.5f, 10f), line.width, StrokeCap.Round)
            drawLine(c, o(7f, 19.5f), o(17f, 19.5f), line.width, StrokeCap.Round)
        }
        G.FLAME -> {
            val p = Path().apply {
                moveTo(12f * s, 2.5f * s)
                cubicTo(13f * s, 7f * s, 19f * s, 9f * s, 19f * s, 14.5f * s)
                cubicTo(19f * s, 18.5f * s, 16f * s, 21.5f * s, 12f * s, 21.5f * s)
                cubicTo(8f * s, 21.5f * s, 5f * s, 18.5f * s, 5f * s, 14.5f * s)
                cubicTo(5f * s, 11f * s, 8f * s, 9.5f * s, 8.5f * s, 6.5f * s)
                cubicTo(10.5f * s, 8f * s, 11f * s, 10f * s, 11f * s, 11.5f * s)
                cubicTo(12.5f * s, 9f * s, 12.5f * s, 5.5f * s, 12f * s, 2.5f * s); close()
            }
            drawPath(p, c)
        }
        G.HEART -> {
            val p = Path().apply {
                moveTo(12f * s, 20.5f * s)
                cubicTo(4f * s, 15f * s, 2f * s, 11f * s, 2.5f * s, 8f * s)
                cubicTo(3f * s, 4.5f * s, 8.5f * s, 3f * s, 12f * s, 7.5f * s)
                cubicTo(15.5f * s, 3f * s, 21f * s, 4.5f * s, 21.5f * s, 8f * s)
                cubicTo(22f * s, 11f * s, 20f * s, 15f * s, 12f * s, 20.5f * s); close()
            }
            drawPath(p, c)
        }
        G.STEPS -> {
            drawOval(c, o(4f, 3f), Size(6f * s, 9f * s))
            drawOval(c, o(14f, 8f), Size(6f * s, 9f * s))
            drawOval(c, o(4.8f, 13.5f), Size(4.4f * s, 3.4f * s))
            drawOval(c, o(14.8f, 18.5f), Size(4.4f * s, 3.4f * s))
        }
        G.BED -> {
            drawLine(c, o(2.5f, 6f), o(2.5f, 19f), line.width, StrokeCap.Round)
            drawLine(c, o(2.5f, 16f), o(21.5f, 16f), line.width, StrokeCap.Round)
            drawLine(c, o(21.5f, 16f), o(21.5f, 19f), line.width, StrokeCap.Round)
            drawRoundRect(c, o(10f, 10f), Size(11.5f * s, 6f * s), CornerRadius(2.5f * s), style = line)
            drawCircle(c, 2.2f * s, o(6.5f, 12.5f))
        }
        G.CHECK -> {
            val p = Path().apply { moveTo(5f * s, 12.5f * s); lineTo(10f * s, 17.5f * s); lineTo(19.5f * s, 7f * s) }
            drawPath(p, c, style = Stroke(2.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        G.CHEVRON -> {
            val p = Path().apply { moveTo(9f * s, 5f * s); lineTo(16f * s, 12f * s); lineTo(9f * s, 19f * s) }
            drawPath(p, c, style = line)
        }
        G.BELL -> {
            val p = Path().apply {
                moveTo(5f * s, 17f * s); lineTo(19f * s, 17f * s)
                cubicTo(17.5f * s, 15.5f * s, 17.5f * s, 13f * s, 17.5f * s, 10.5f * s)
                cubicTo(17.5f * s, 7f * s, 15f * s, 4.5f * s, 12f * s, 4.5f * s)
                cubicTo(9f * s, 4.5f * s, 6.5f * s, 7f * s, 6.5f * s, 10.5f * s)
                cubicTo(6.5f * s, 13f * s, 6.5f * s, 15.5f * s, 5f * s, 17f * s); close()
            }
            drawPath(p, c, style = line)
            drawLine(c, o(10.5f, 20f), o(13.5f, 20f), line.width, StrokeCap.Round)
        }
        G.PIN -> {
            val p = Path().apply {
                moveTo(12f * s, 21.5f * s)
                cubicTo(7f * s, 15f * s, 5f * s, 12f * s, 5f * s, 9.5f * s)
                cubicTo(5f * s, 5.5f * s, 8f * s, 2.5f * s, 12f * s, 2.5f * s)
                cubicTo(16f * s, 2.5f * s, 19f * s, 5.5f * s, 19f * s, 9.5f * s)
                cubicTo(19f * s, 12f * s, 17f * s, 15f * s, 12f * s, 21.5f * s); close()
            }
            drawPath(p, c, style = line)
            drawCircle(c, 2.4f * s, o(12f, 9.5f), style = line)
        }
        G.PAPERPLANE -> {
            val p = Path().apply {
                moveTo(2.5f * s, 11f * s); lineTo(21.5f * s, 3f * s); lineTo(15f * s, 21f * s)
                lineTo(11.5f * s, 13f * s); close()
            }
            drawPath(p, c, style = line)
            drawLine(c, o(11.5f, 13f), o(21.5f, 3f), line.width, StrokeCap.Round)
        }
    }
}
