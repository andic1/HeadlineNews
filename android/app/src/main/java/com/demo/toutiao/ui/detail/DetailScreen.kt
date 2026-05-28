package com.demo.toutiao.ui.detail

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.ShimmerBase
import com.demo.toutiao.ui.theme.ShimmerHighlight
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DetailScreen(
    newsItem: NewsItem,
    onBack: () -> Unit,
) {
    val url = newsItem.originalUrl ?: ""
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(newsItem.source ?: "文章详情") }

    // 处理系统返回键：WebView 可后退则后退，否则退出页面
    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pageTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        val wv = webViewRef
                        if (wv != null && wv.canGoBack()) {
                            wv.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* share */ }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "分享",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(onClick = { /* more */ }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White,
                ),
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (url.isNotBlank()) {
                // 加载中骨架屏（WebView 开始前显示）
                AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(tween(100)),
                    exit = fadeOut(tween(300)),
                ) {
                    DetailSkeleton()
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                            settings.loadsImagesAutomatically = true
                            settings.defaultTextEncodingName = "UTF-8"
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.databaseEnabled = true
                            settings.allowFileAccess = false
                            isVerticalScrollBarEnabled = true
                            isHorizontalScrollBarEnabled = false
                            setBackgroundColor(android.graphics.Color.WHITE)
                            // 平滑滚动
                            isNestedScrollingEnabled = true
                            overScrollMode = WebView.OVER_SCROLL_NEVER

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, loadedUrl: String?, favicon: Bitmap?) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    if (reqUrl.startsWith("http://") || reqUrl.startsWith("https://")) {
                                        view?.loadUrl(reqUrl)
                                        return true
                                    }
                                    return true
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                        pageTitle = title
                                    }
                                }
                            }

                            webViewRef = this
                            loadUrl(url)
                        }
                    },
                )
            } else {
                // 没有 URL
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无链接", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // 加载进度条
            AnimatedVisibility(
                visible = isLoading && url.isNotBlank(),
                enter = fadeIn(),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = ToutiaoRed,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}

/**
 * 详情页加载骨架屏
 */
@Composable
private fun DetailSkeleton() {
    val transition = rememberInfiniteTransition(label = "detailSkeleton")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "detailSkeletonOffset",
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = Offset(translateAnim.value - 300f, 0f),
        end = Offset(translateAnim.value, 0f),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),
    ) {
        // 标题占位
        SkeletonBlock(shimmerBrush, Modifier.fillMaxWidth().height(22.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(shimmerBrush, Modifier.fillMaxWidth(0.75f).height(22.dp))
        Spacer(Modifier.height(16.dp))
        // 来源/时间
        SkeletonBlock(shimmerBrush, Modifier.width(100.dp).height(14.dp))
        Spacer(Modifier.height(24.dp))
        // 正文段落
        repeat(5) { idx ->
            SkeletonBlock(
                shimmerBrush,
                Modifier.fillMaxWidth(if (idx == 4) 0.6f else 1f).height(14.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        // 图片占位
        SkeletonBlock(
            shimmerBrush,
            Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(20.dp))
        // 更多段落
        repeat(3) { idx ->
            SkeletonBlock(
                shimmerBrush,
                Modifier.fillMaxWidth(if (idx == 2) 0.5f else 1f).height(14.dp)
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SkeletonBlock(brush: Brush, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}
