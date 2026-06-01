package com.headline.news.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headline.news.ui.theme.DividerColor
import com.headline.news.ui.theme.TextCaption
import com.headline.news.ui.theme.TextPrimary
import com.headline.news.ui.theme.TopBarBg
import com.headline.news.ui.theme.BrandRed

@Composable
fun CategoryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 14.dp,
        containerColor = TopBarBg,
        contentColor = TextPrimary,
        indicator = {},
        divider = {
            HorizontalDivider(color = DividerColor, thickness = 0.6.dp)
        },
    ) {
        tabs.forEachIndexed { i, name ->
            val isSelected = i == selectedIndex
            val chipColor by animateColorAsState(
                targetValue = if (isSelected) BrandRed else Color.Transparent,
                animationSpec = tween(180),
                label = "tabChipColor",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else TextCaption,
                animationSpec = tween(180),
                label = "tabTextColor",
            )
            val horizontalPadding by animateDpAsState(
                targetValue = if (isSelected) 17.dp else 13.dp,
                animationSpec = tween(180),
                label = "tabPadding",
            )

            Tab(
                selected = isSelected,
                onClick = { onSelect(i) },
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp),
                text = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(chipColor)
                            .padding(horizontal = horizontalPadding, vertical = 8.dp),
                    ) {
                        Text(
                            text = name,
                            fontSize = if (isSelected) 15.sp else 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = textColor,
                            maxLines = 1,
                        )
                    }
                },
            )
        }
    }
}
