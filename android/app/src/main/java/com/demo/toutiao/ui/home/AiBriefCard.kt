package com.demo.toutiao.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.ui.ai.AiUiState
import com.demo.toutiao.ui.theme.ShimmerBase
import com.demo.toutiao.ui.theme.ShimmerHighlight
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@Composable
fun AiBriefCard(
    state: AiUiState<AiDailyBriefResponse>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF7F0), Color(0xFFF7FBFF)),
                    start = Offset.Zero,
                    end = Offset(700f, 320f),
                ),
                shape = RoundedCornerShape(22.dp),
            )
            .border(1.dp, Color(0x14D63B30), RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(ToutiaoRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = ToutiaoRed,
                )
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = "AI 今日速读",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "从当前新闻流里提炼真正值得看的重点",
                    color = TextCaption,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        when (state) {
            AiUiState.Idle, AiUiState.Loading -> AiBriefSkeleton()
            is AiUiState.Error -> Text(
                text = state.message,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
            is AiUiState.Success -> {
                state.data.items.take(3).forEachIndexed { index, item ->
                    BriefLine(
                        index = index + 1,
                        title = item.title,
                        reason = item.reason,
                    )
                    if (index < state.data.items.take(3).lastIndex) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BriefLine(index: Int, title: String, reason: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = ToutiaoRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (reason.isNotBlank()) {
                Text(
                    text = reason,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AiBriefSkeleton() {
    val transition = rememberInfiniteTransition(label = "aiBriefSkeleton")
    val offset = transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "aiBriefSkeletonOffset",
    )
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(offset.value - 220f, 0f),
        end = Offset(offset.value, 0f),
    )
    repeat(3) { index ->
        Box(
            modifier = Modifier
                .fillMaxWidth(if (index == 2) 0.7f else 1f)
                .height(14.dp)
                .background(brush, RoundedCornerShape(8.dp)),
        )
        if (index < 2) Spacer(Modifier.height(10.dp))
    }
}
