package com.demo.toutiao.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.ui.theme.ToutiaoRed

/** 红底顶栏：状态条 + 天气 + 搜索框 + 登录入口（静态） */
@Composable
fun HomeTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ToutiaoRed)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("9:41", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Text("14° 良", color = Color.White, fontSize = 13.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFFFFD7D7).copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("习近平主席重要讲话", color = Color.White, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Text("A登录", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}
