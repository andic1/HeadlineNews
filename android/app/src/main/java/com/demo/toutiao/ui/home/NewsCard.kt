package com.demo.toutiao.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.demo.toutiao.data.model.displayPublishTime
import com.demo.toutiao.ui.theme.CardBg
import com.demo.toutiao.ui.theme.DividerColor
import com.demo.toutiao.ui.theme.ShimmerBase
import com.demo.toutiao.ui.theme.ShimmerHighlight
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@Composable
fun NewsCard(
    item: NewsItem,
    legacyStyle: Boolean = false,
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(120),
        label = "cardScale",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .scale(scale)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color(0x0C000000),
                    spotColor = Color(0x0C000000),
                )
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, DividerColor.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(color = Color(0x10000000)),
                ) { onNewsClick(item) }
                .padding(14.dp),
        ) {
            if (legacyStyle) {
                when (item.layoutType) {
                    LayoutType.BIG_IMAGE -> LegacyBigImageBody(item)
                    LayoutType.TEXT_WITH_THUMB -> LegacyTextWithThumbBody(item)
                    else -> LegacyTextOnlyBody(item)
                }
            } else {
                when (item.layoutType) {
                    LayoutType.BIG_IMAGE -> BigImageBody(item)
                    LayoutType.TEXT_WITH_THUMB -> TextWithThumbBody(item)
                    else -> TextOnlyBody(item)
                }
            }
        }
    }
}

@Composable
private fun LegacyTextOnlyBody(item: NewsItem) {
    Column {
        Headline(item)
        Spacer(Modifier.height(10.dp))
        MetaRow(item)
    }
}

@Composable
private fun LegacyTextWithThumbBody(item: NewsItem) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.fillMaxWidth(0.68f),
        ) {
            Headline(item)
            Spacer(Modifier.height(10.dp))
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
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0)),
                    )
                },
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
            )
        }
    }
}

@Composable
private fun LegacyBigImageBody(item: NewsItem) {
    Column {
        Headline(item)
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
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0)),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
            )
            Spacer(Modifier.height(10.dp))
        }
        MetaRow(item)
    }
}

@Composable
private fun TextOnlyBody(item: NewsItem) {
    Column {
        Headline(item)
        item.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            SummaryText(text = it)
        }
        Spacer(Modifier.height(12.dp))
        MetaRow(item)
    }
}

@Composable
private fun TextWithThumbBody(item: NewsItem) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier.fillMaxWidth(0.68f),
        ) {
            Headline(item)
            item.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                SummaryText(text = it, maxLines = 2)
            }
            Spacer(Modifier.height(10.dp))
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
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .size(width = 96.dp, height = 64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0)),
                    )
                },
                modifier = Modifier
                    .size(width = 96.dp, height = 64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
            )
        }
    }
}

@Composable
private fun BigImageBody(item: NewsItem) {
    Column {
        Headline(item)
        item.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            SummaryText(text = it)
        }
        Spacer(Modifier.height(12.dp))
        if (!item.imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F0F0)),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5)),
            )
            Spacer(Modifier.height(12.dp))
        }
        MetaRow(item)
    }
}

@Composable
private fun Headline(item: NewsItem) {
    Text(
        item.title,
        color = TextPrimary,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 24.sp,
    )
}

@Composable
private fun SummaryText(text: String, maxLines: Int = 2) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MetaRow(item: NewsItem) {
    val displayTime = item.displayPublishTime()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!item.source.isNullOrBlank()) {
            Text(
                text = item.source,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }

        if (!displayTime.isNullOrBlank()) {
            if (!item.source.isNullOrBlank()) {
                Text(
                    text = " \u00b7 ",
                    color = TextCaption,
                    fontSize = 11.sp,
                )
            }
            Text(
                text = displayTime,
                color = TextCaption,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

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
