package com.demo.toutiao.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ToutiaoRed = Color(0xFFE23D31)
val ToutiaoRedLight = Color(0xFFFF725E)
val ToutiaoRedBg = Color(0xFFFFF1ED)
val Bg = Color(0xFFF4F1EA)
val CardBg = Color(0xFFFFFCF7)
val TextPrimary = Color(0xFF171512)
val TextSecondary = Color(0xFF575047)
val TextCaption = Color(0xFF8A8175)
val DividerColor = Color(0x1A171512)
val TopBarBg = Color(0xFFFFFCF7)
val TabIndicatorColor = Color(0xFFE23D31)
val SearchBarBg = Color(0xFFF1E9DD)
val ShimmerBase = Color(0xFFE9E0D3)
val ShimmerHighlight = Color(0xFFFFFBF4)

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
