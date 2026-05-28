package com.demo.toutiao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ToutiaoRed = Color(0xFFD63B30)
val ToutiaoRedLight = Color(0xFFE85D52)
val ToutiaoRedBg = Color(0xFFFEF2F2)
val Bg = Color(0xFFF4F5F6)
val CardBg = Color.White
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF666666)
val TextCaption = Color(0xFF999999)
val DividerColor = Color(0xFFEEEEEE)
val TopBarBg = Color.White
val TabIndicatorColor = Color(0xFFD63B30)
val SearchBarBg = Color(0xFFF5F6F7)
val ShimmerBase = Color(0xFFEEEEEE)
val ShimmerHighlight = Color(0xFFF8F8F8)

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
