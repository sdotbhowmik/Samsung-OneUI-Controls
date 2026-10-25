package com.virtualkeys.oneui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Accents = listOf(
    Color(0xFF4C8DFF),
    Color(0xFF34C759),
    Color(0xFFB07CFF),
    Color(0xFFFF9F45),
    Color(0xFFFF6070),
)

data class OneUiPalette(
    val panel: Color,
    val card: Color,
    val track: Color,
    val text: Color,
    val secondary: Color,
    val accent: Color,
    val onAccent: Color,
    val scrim: Color,
    val divider: Color,
    val background: Color,
)

fun oneUiPalette(dark: Boolean, accent: Color): OneUiPalette = if (dark) {
    OneUiPalette(
        panel = Color(0xF21C1C1E),
        card = Color(0xFF2A2A2D),
        track = Color(0xFF3A3A3F),
        text = Color(0xFFF2F2F4),
        secondary = Color(0xFF9B9BA1),
        accent = accent,
        onAccent = Color(0xFF101014),
        scrim = Color.Black.copy(alpha = 0.45f),
        divider = Color(0x1FFFFFFF),
        background = Color(0xFF0E0E10),
    )
} else {
    OneUiPalette(
        panel = Color(0xF7F4F4F6),
        card = Color(0xFFFFFFFF),
        track = Color(0xFFE3E3E8),
        text = Color(0xFF111114),
        secondary = Color(0xFF6E6E76),
        accent = accent,
        onAccent = Color.White,
        scrim = Color.Black.copy(alpha = 0.35f),
        divider = Color(0x14000000),
        background = Color(0xFFF5F5F7),
    )
}

val LocalOneUi = staticCompositionLocalOf { oneUiPalette(true, Accents[0]) }

val PanelShape = RoundedCornerShape(30.dp)
val MediaCardShape = RoundedCornerShape(24.dp)
val InnerCardShape = RoundedCornerShape(20.dp)

@Composable
fun OneUiTheme(
    dark: Boolean,
    accentIndex: Int,
    content: @Composable () -> Unit,
) {
    val accent = Accents.getOrElse(accentIndex) { Accents[0] }
    val palette = oneUiPalette(dark, accent)
    CompositionLocalProvider(LocalOneUi provides palette) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = accent,
                    background = palette.background,
                    surface = palette.card,
                    onSurface = palette.text,
                )
            } else {
                lightColorScheme(
                    primary = accent,
                    background = palette.background,
                    surface = palette.card,
                    onSurface = palette.text,
                )
            },
            content = content,
        )
    }
}
