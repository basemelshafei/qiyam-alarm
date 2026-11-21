package com.basem.qiyam.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.basem.qiyam.R

/** Apple's system colours (iOS 17 Human Interface Guidelines), light and dark. */
data class Palette(
    val background: Color,   // systemGroupedBackground
    val card: Color,         // secondarySystemGroupedBackground
    val separator: Color,    // opaque separator / ring track
    val fill: Color,         // tertiarySystemFill (unselected chips, fields)
    val label: Color,
    val secondary: Color,    // secondaryLabel
    val tint: Color,         // systemBlue
    val green: Color,
    val orange: Color,
    val red: Color,
    val bar: Color,          // tab bar
)

private val Light = Palette(
    background = Color(0xFFF2F2F7), card = Color.White, separator = Color(0xFFE5E5EA),
    fill = Color(0x1F767680), label = Color(0xFF000000), secondary = Color(0xFF8A8A8E),
    tint = Color(0xFF007AFF), green = Color(0xFF34C759), orange = Color(0xFFFF9500), red = Color(0xFFFF3B30),
    bar = Color(0xF7F9F9F9),
)

private val Dark = Palette(
    background = Color(0xFF000000), card = Color(0xFF1C1C1E), separator = Color(0xFF38383A),
    fill = Color(0x3D767680), label = Color(0xFFFFFFFF), secondary = Color(0xFF8D8D93),
    tint = Color(0xFF0A84FF), green = Color(0xFF30D158), orange = Color(0xFFFF9F0A), red = Color(0xFFFF453A),
    bar = Color(0xF0161618),
)

/**
 * The app's colours. Names are kept from the first design so every screen picks up the new look:
 * Ink = page background, Card = card, CardEdge = separators, Gold = the accent (systemBlue).
 */
object Night {
    @Volatile internal var p: Palette = Light
    val isDark get() = p === Dark

    val Ink get() = p.background
    val Card get() = p.card
    val CardEdge get() = p.separator
    val Fill get() = p.fill
    val Text get() = p.label
    val Muted get() = p.secondary
    val Gold get() = p.tint
    val OnAccent get() = Color.White
    val Good get() = p.green
    val Warn get() = p.orange
    val Dawn get() = p.red
    val Bar get() = p.bar

    val sky: Brush get() = SolidColor(p.background)
}

/** Apple Fitness-style ring colours, one per area of life. */
object Rings {
    val Deen = Color(0xFF30D158)
    val Body = Color(0xFFFA114F)
    val Career = Color(0xFF0A84FF)
    val Wealth = Color(0xFFFFD60A)
}

/** Fixed night-sky colours for the Qiyam dial and the alarm page, which are always dark. */
object Sky {
    val Ink = Color(0xFF070B1A)
    val Deep = Color(0xFF0E1530)
    val Indigo = Color(0xFF1B2552)
    val Edge = Color(0xFF2A3466)
    val Gold = Color(0xFFE8C27A)
    val Dawn = Color(0xFFF2A47A)
    val Text = Color(0xFFECEFF8)
    val Muted = Color(0xFF8E97B8)
    val gradient = Brush.verticalGradient(listOf(Ink, Deep, Indigo))
}

/** Inter: the closest open-source match to Apple's San Francisco typeface. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography()
private val InterTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Inter),
    displayMedium = base.displayMedium.copy(fontFamily = Inter),
    displaySmall = base.displaySmall.copy(fontFamily = Inter),
    headlineLarge = base.headlineLarge.copy(fontFamily = Inter),
    headlineMedium = base.headlineMedium.copy(fontFamily = Inter),
    headlineSmall = base.headlineSmall.copy(fontFamily = Inter),
    titleLarge = base.titleLarge.copy(fontFamily = Inter),
    titleMedium = base.titleMedium.copy(fontFamily = Inter),
    titleSmall = base.titleSmall.copy(fontFamily = Inter),
    bodyLarge = base.bodyLarge.copy(fontFamily = Inter, letterSpacing = base.bodyLarge.letterSpacing * 0),
    bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
    bodySmall = base.bodySmall.copy(fontFamily = Inter),
    labelLarge = base.labelLarge.copy(fontFamily = Inter),
    labelMedium = base.labelMedium.copy(fontFamily = Inter),
    labelSmall = base.labelSmall.copy(fontFamily = Inter),
)

/** Follows the phone's light/dark setting, like Apple's apps do. */
@Composable
fun QiyamTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    Night.p = if (dark) Dark else Light
    val c = Night.p
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(primary = c.tint, onPrimary = Color.White, background = c.background, surface = c.card, onSurface = c.label, onBackground = c.label)
        } else {
            lightColorScheme(primary = c.tint, onPrimary = Color.White, background = c.background, surface = c.card, onSurface = c.label, onBackground = c.label)
        },
        typography = InterTypography,
        content = content,
    )
}
