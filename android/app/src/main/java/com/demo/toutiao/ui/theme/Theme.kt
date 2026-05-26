package com.demo.toutiao.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ToutiaoRed = Color(0xFFFF2A2A)
val Bg = Color(0xFFF5F5F5)
val CardBg = Color.White
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF8A8A8A)
val DividerColor = Color(0xFFEEEEEE)

private val LightColors = lightColorScheme(
    primary = ToutiaoRed,
    onPrimary = Color.White,
    background = Bg,
    surface = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun ToutiaoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
