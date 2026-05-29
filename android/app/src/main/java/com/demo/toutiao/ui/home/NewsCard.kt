package com.demo.toutiao.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.CardBg
import com.demo.toutiao.ui.theme.DividerColor
import com.demo.toutiao.ui.theme.ShimmerBase
import com.demo.toutiao.ui.theme.ShimmerHighlight
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary

@Composable
fun NewsCard(
    item: NewsItem,
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "cardScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(CardBg)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color(0x10000000)),
            ) { onNewsClick(item) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when (item.layoutType) {
            LayoutType.BIG_IMAGE -> BigImageBody(item)
            LayoutType.TEXT_WITH_THUMB -> TextWithThumbBody(item)
            else -> TextOnlyBody(item)
        }
    }
    // 分割线
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(DividerColor)
    )
}

@Composable
private fun TextOnlyBody(item: NewsItem) {
    Column {
        Text(
            item.title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(10.dp))
        MetaRow(item)
    }
}

@Composable
private fun TextWithThumbBody(item: NewsItem) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 78.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                item.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(8.dp))
            MetaRow(item)
        }
        Spacer(Modifier.width(12.dp))
        if (!item.imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .size(width = 120.dp, height = 80.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 80.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF0F0F0))
                    )
                },
                modifier = Modifier
                    .size(width = 120.dp, height = 80.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF5F5F5)),
            )
        }
    }
}

@Composable
private fun BigImageBody(item: NewsItem) {
    Column {
        Text(
            item.title,
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (!item.imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0))
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
            )
            Spacer(Modifier.height(10.dp))
        }
        MetaRow(item)
    }
}

@Composable
private fun MetaRow(item: NewsItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!item.source.isNullOrBlank()) {
            Text(
                item.source,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        if (!item.source.isNullOrBlank() && !item.publishTime.isNullOrBlank()) {
            Text(
                " · ",
                color = TextCaption,
                fontSize = 11.sp,
            )
        }
        if (!item.publishTime.isNullOrBlank()) {
            Text(
                item.publishTime,
                color = TextCaption,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

/** 真正的 Shimmer 动画占位块 */
@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val brush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value, 0f),
    )
    Box(
        modifier = modifier.background(brush),
    )
}
