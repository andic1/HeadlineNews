package com.demo.toutiao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.demo.toutiao.ui.detail.DetailScreen
import com.demo.toutiao.ui.home.HomeScreen
import com.demo.toutiao.ui.theme.ToutiaoTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToutiaoTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onNewsClick = { title, source, url ->
                                val encTitle = URLEncoder.encode(title, "UTF-8")
                                val encSource = URLEncoder.encode(source ?: "", "UTF-8")
                                val encUrl = URLEncoder.encode(url, "UTF-8")
                                navController.navigate("detail/$encTitle/$encSource/$encUrl")
                            }
                        )
                    }
                    composable(
                        route = "detail/{title}/{source}/{url}",
                        arguments = listOf(
                            navArgument("title") { type = NavType.StringType },
                            navArgument("source") { type = NavType.StringType },
                            navArgument("url") { type = NavType.StringType },
                        ),
                    ) { entry ->
                        val title = URLDecoder.decode(entry.arguments?.getString("title") ?: "", "UTF-8")
                        val source = URLDecoder.decode(entry.arguments?.getString("source") ?: "", "UTF-8")
                            .ifBlank { null }
                        val url = URLDecoder.decode(entry.arguments?.getString("url") ?: "", "UTF-8")
                        DetailScreen(
                            title = title,
                            source = source,
                            url = url,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
