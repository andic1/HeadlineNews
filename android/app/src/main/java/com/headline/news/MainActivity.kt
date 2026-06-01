package com.headline.news

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.headline.news.ui.detail.DetailScreen
import com.headline.news.ui.home.HomeScreen
import com.headline.news.ui.home.HomeViewModel
import com.headline.news.ui.splash.SplashScreen
import com.headline.news.ui.theme.HeadlineNewsTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        super.onCreate(savedInstanceState)
        setContent {
            HeadlineNewsTheme {
                var showSplash by remember { mutableStateOf(true) }
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    delay(900)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    val viewModel: HomeViewModel = hiltViewModel()
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                tween(250, easing = FastOutSlowInEasing),
                            ) + fadeIn(tween(200))
                        },
                        exitTransition = {
                            fadeOut(tween(150))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                tween(250, easing = FastOutSlowInEasing),
                                initialOffset = { it / 4 },
                            ) + fadeIn(tween(200))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                tween(250, easing = FastOutSlowInEasing),
                            ) + fadeOut(tween(150))
                        },
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNewsClick = { item ->
                                    viewModel.selectItem(item)
                                    navController.navigate("detail")
                                },
                            )
                        }
                        composable("detail") {
                            val item by viewModel.selectedItem.collectAsState()
                            item?.let {
                                DetailScreen(
                                    newsItem = it,
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
