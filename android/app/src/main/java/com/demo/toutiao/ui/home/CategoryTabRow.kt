package com.demo.toutiao.ui.home

import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.ui.theme.ToutiaoRed

@Composable
fun CategoryTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 8.dp,
        containerColor = ToutiaoRed,
        contentColor = Color.White,
        divider = {},
    ) {
        tabs.forEachIndexed { i, name ->
            Tab(
                selected = i == selectedIndex,
                onClick = { onSelect(i) },
                text = {
                    Text(
                        name,
                        fontSize = if (i == selectedIndex) 17.sp else 15.sp,
                        fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (i == selectedIndex) Color.White else Color.White.copy(alpha = 0.7f),
                    )
                }
            )
        }
    }
}
