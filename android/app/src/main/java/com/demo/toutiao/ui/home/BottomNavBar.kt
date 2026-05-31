package com.demo.toutiao.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Storefront
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

@Composable
fun BottomNavBar() {
    Column {
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg)
                .navigationBarsPadding()
                .height(62.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(Icons.Outlined.Home, "\u9996\u9875", selected = true)
            BottomItem(Icons.Outlined.SmartDisplay, "\u89c6\u9891", selected = false)
            BottomItem(Icons.Outlined.Storefront, "\u5546\u57ce", selected = false)
            BottomItem(Icons.Outlined.PlayCircle, "\u653e\u6620\u5385", selected = false)
            BottomItem(Icons.Outlined.Person, "\u6211\u7684", selected = false)
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean) {
    val animatedTint by animateColorAsState(
        targetValue = if (selected) ToutiaoRed else TextCaption,
        animationSpec = tween(200),
        label = "tint",
    )
    val animatedTextColor by animateColorAsState(
        targetValue = if (selected) TextPrimary else TextCaption,
        animationSpec = tween(200),
        label = "textColor",
    )
    val pillColor by animateColorAsState(
        targetValue = if (selected) ToutiaoRed.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(200),
        label = "pillColor",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .widthIn(min = 54.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
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
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = animatedTextColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
