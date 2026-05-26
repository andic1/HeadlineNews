package com.demo.toutiao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.demo.toutiao.ui.home.HomeScreen
import com.demo.toutiao.ui.theme.ToutiaoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToutiaoTheme {
                HomeScreen()
            }
        }
    }
}
