package com.demo.toutiao.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.data.api.AiBriefItem
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.ai.AiUiState
import com.demo.toutiao.ui.theme.CardBg
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
    onItemClick: (AiBriefItem) -> Unit = {},
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    var isDismissed by rememberSaveable { mutableStateOf(false) }
    if (isDismissed) return

    Column(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFF4EE), Color(0xFFF3FAFF)),
                    start = Offset.Zero,
                    end = Offset(760f, 320f),
                ),
            )
            .border(1.dp, ToutiaoRed.copy(alpha = 0.10f), RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(animationSpec = tween(220))
            .padding(16.dp),
    ) {
        AiBriefHeader(
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded },
            onDismiss = { isDismissed = true },
        )

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(Modifier.height(13.dp))
                when (state) {
                    AiUiState.Idle, AiUiState.Loading -> AiBriefSkeleton()
                    is AiUiState.Error -> AiBriefError(state.message)
                    is AiUiState.Success -> {
                        val briefItems = state.data.items.take(3)
                        if (briefItems.isEmpty()) {
                            AiBriefError("\u6682\u65f6\u6ca1\u6709\u8db3\u591f\u70ed\u70b9\u53ef\u4ee5\u901f\u8bfb")
                        } else {
                            briefItems.forEachIndexed { index, item ->
                                BriefLine(
                                    index = index + 1,
                                    item = item,
                                    onClick = { onItemClick(item) },
                                )
                                if (index < briefItems.lastIndex) {
                                    Spacer(Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiBriefHeader(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(ToutiaoRed.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = ToutiaoRed,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 11.dp)
                .weight(1f),
        ) {
            Text(
                text = "AI \u4eca\u65e5\u901f\u8bfb",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = if (isExpanded) {
                    "\u4ece\u5f53\u524d\u65b0\u95fb\u6d41\u91cc\u63d0\u70bc\u771f\u6b63\u503c\u5f97\u770b\u7684\u91cd\u70b9"
                } else {
                    "\u5df2\u6536\u8d77\uff0c\u70b9\u51fb\u5c55\u5f00"
                },
                color = TextCaption,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HeaderIconButton(
            icon = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (isExpanded) "\u6536\u8d77 AI \u901f\u8bfb" else "\u5c55\u5f00 AI \u901f\u8bfb",
            onClick = onToggle,
        )
        HeaderIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = "\u5173\u95ed AI \u901f\u8bfb",
            onClick = onDismiss,
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CardBg.copy(alpha = 0.72f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AiBriefError(message: String) {
    Text(
        text = message,
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}

@Composable
private fun BriefLine(index: Int, item: AiBriefItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = ToutiaoRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )
        Column {
            Text(
                text = item.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.reason.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.reason,
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

fun AiBriefItem.toFallbackNewsItem(category: String): NewsItem {
    val stableId = newsId.ifBlank { url ?: title }
    return NewsItem(
        id = stableId,
        category = category,
        title = title,
        description = reason,
        source = source,
        imageUrl = imageUrl,
        originalUrl = url,
        publishTime = publishTime,
        layoutType = LayoutType.TEXT_ONLY,
    )
}

@Composable
private fun AiBriefSkeleton() {
    val transition = rememberInfiniteTransition(label = "aiBriefSkeleton")
    val offset = transition.animateFloat(
        initialValue = 0f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = LinearEasing),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(12.dp)
                    .background(brush, RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index == 2) 0.72f else 1f)
                    .height(14.dp)
                    .background(brush, RoundedCornerShape(8.dp)),
            )
        }
        if (index < 2) Spacer(Modifier.height(12.dp))
    }
}
