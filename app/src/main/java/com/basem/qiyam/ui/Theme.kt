package com.basem.qiyam.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Night {
    val Ink = Color(0xFF070B1A)
    val Deep = Color(0xFF0E1530)
    val Indigo = Color(0xFF1B2552)
    val Card = Color(0xFF121A38)
    val CardEdge = Color(0xFF243063)
    val Gold = Color(0xFFE8C27A)
    val GoldSoft = Color(0x33E8C27A)
    val Dawn = Color(0xFFF2A47A)
    val Text = Color(0xFFECEFF8)
    val Muted = Color(0xFF8E97B8)
    val Good = Color(0xFF7FD4A8)
    val Warn = Color(0xFFF0B067)

    val sky = Brush.verticalGradient(listOf(Ink, Deep, Indigo))
}

@Composable
fun QiyamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Night.Gold,
            onPrimary = Night.Ink,
            background = Night.Ink,
            surface = Night.Card,
            onSurface = Night.Text,
            onBackground = Night.Text,
            secondary = Night.Dawn,
        ),
        content = content,
    )
}
