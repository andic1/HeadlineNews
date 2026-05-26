package com.demo.toutiao.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.CardBg
import com.demo.toutiao.ui.theme.DividerColor
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary

@Composable
fun NewsCard(item: NewsItem) {
    val context = LocalContext.current
    val openOriginal = {
        item.originalUrl?.let { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .clickable { openOriginal() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        when (item.layoutType) {
            LayoutType.BIG_IMAGE -> BigImageBody(item)
            LayoutType.TEXT_WITH_THUMB -> TextWithThumbBody(item)
            else -> TextOnlyBody(item)
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
    }
}

@Composable
private fun TextOnlyBody(item: NewsItem) {
    Column {
        Text(
            item.title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        MetaRow(item)
    }
}

@Composable
private fun TextWithThumbBody(item: NewsItem) {
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            MetaRow(item)
        }
        Spacer(Modifier.width(10.dp))
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 108.dp, height = 76.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DividerColor),
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
        )
        Spacer(Modifier.height(8.dp))
        if (!item.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(DividerColor),
            )
            Spacer(Modifier.height(8.dp))
        }
        MetaRow(item)
    }
}

@Composable
private fun MetaRow(item: NewsItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!item.source.isNullOrBlank()) {
            Text(item.source, color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
        }
        if (!item.publishTime.isNullOrBlank()) {
            Text(item.publishTime, color = TextSecondary, fontSize = 12.sp)
        }
    }
}
