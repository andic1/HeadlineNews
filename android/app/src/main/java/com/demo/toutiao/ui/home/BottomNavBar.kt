package com.demo.toutiao.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

/** 底栏静态壳：5 个 Tab，仅"首页"高亮，其他不可交互 */
@Composable
fun BottomNavBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomItem(Icons.Default.Home, "首页", selected = true)
        BottomItem(Icons.Default.PlayCircle, "西瓜视频", selected = false)
        BottomItem(Icons.Default.Movie, "放映厅", selected = false)
        BottomItem(Icons.Default.Star, "未登录", selected = false)
        BottomItem(Icons.Default.Person, "我的", selected = false)
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean) {
    val tint = if (selected) ToutiaoRed else TextSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = tint, fontSize = 10.sp)
    }
}
