package com.demo.toutiao.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.ui.theme.CardBg
import com.demo.toutiao.ui.theme.DividerColor
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.ToutiaoRed
import com.demo.toutiao.ui.theme.ToutiaoRedBg

@Composable
fun BottomNavBar() {
    Column {
        HorizontalDivider(color = DividerColor, thickness = 0.6.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .navigationBarsPadding()
                .height(66.dp)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(Icons.Outlined.Home, "\u9996\u9875", selected = true)
            BottomItem(Icons.Outlined.Whatshot, "\u70ed\u699c", selected = false)
            BottomItem(Icons.Outlined.AutoAwesome, "AI\u901f\u8bfb", selected = false)
            BottomItem(Icons.Outlined.BookmarkBorder, "\u6536\u85cf", selected = false)
            BottomItem(Icons.Outlined.Person, "\u6211\u7684", selected = false)
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean) {
    val animatedTint by animateColorAsState(
        targetValue = if (selected) ToutiaoRed else TextCaption,
        animationSpec = tween(180),
        label = "bottomTint",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextCaption,
        animationSpec = tween(180),
        label = "bottomTextColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) ToutiaoRedBg else Color.Transparent,
        animationSpec = tween(180),
        label = "bottomPillColor",
    )
    val iconSize by animateDpAsState(
        targetValue = if (selected) 25.dp else 23.dp,
        animationSpec = tween(180),
        label = "bottomIconSize",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .widthIn(min = 58.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(pillColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {},
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = animatedTint,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = animatedTextColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
