package com.demo.toutiao.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.ui.theme.DividerColor
import com.demo.toutiao.ui.theme.TabIndicatorColor
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary

/** 白底分类栏：选中项红色加粗 + 红色短下划线指示器 */
@Composable
fun CategoryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 4.dp,
        containerColor = Color.White,
        contentColor = TextPrimary,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    height = 3.dp,
                    color = TabIndicatorColor,
                )
            }
        },
        divider = {
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        },
    ) {
        tabs.forEachIndexed { i, name ->
            val isSelected = i == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (isSelected) TabIndicatorColor else TextSecondary,
                animationSpec = tween(200),
                label = "tabColor",
            )
            Tab(
                selected = isSelected,
                onClick = { onSelect(i) },
                text = {
                    Text(
                        text = name,
                        fontSize = if (isSelected) 16.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = textColor,
                    )
                },
            )
        }
    }
}
