package com.demo.toutiao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ToutiaoRed = Color(0xFFD63B30)
val ToutiaoRedLight = Color(0xFFE85D52)
val ToutiaoRedBg = Color(0xFFFEF2F2)
val Bg = Color(0xFFF7F8FA)
val CardBg = Color.White
val TextPrimary = Color(0xFF111827)
val TextSecondary = Color(0xFF4B5563)
val TextCaption = Color(0xFF6B7280)
val DividerColor = Color(0xFFE5E7EB)
val TopBarBg = Color.White
val TabIndicatorColor = Color(0xFFD63B30)
val SearchBarBg = Color(0xFFF3F5F8)
val ShimmerBase = Color(0xFFEAECEF)
val ShimmerHighlight = Color(0xFFF7F8FA)

private val LightColors = lightColorScheme(
    primary = ToutiaoRed,
    onPrimary = Color.White,
    background = Bg,
    surface = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF7F7F7),
    outline = DividerColor,
)

@Composable
fun ToutiaoTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
