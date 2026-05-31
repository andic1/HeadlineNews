package com.demo.toutiao.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.ai.AiUiState
import com.demo.toutiao.ui.theme.Bg
import com.demo.toutiao.ui.theme.ShimmerBase
import com.demo.toutiao.ui.theme.ShimmerHighlight
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextPrimary
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DetailScreen(
    newsItem: NewsItem,
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val articleState by viewModel.state.collectAsState()
    val aiSummaryState by viewModel.summaryState.collectAsState()
    val aiChatState by viewModel.chatState.collectAsState()

    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(newsItem.title.ifBlank { newsItem.source ?: "原文阅读" }) }
    var question by remember { mutableStateOf("") }

    LaunchedEffect(newsItem.id) {
        viewModel.loadArticle(newsItem)
        pageTitle = newsItem.title.ifBlank { newsItem.source ?: "原文阅读" }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onBack()
        }
    }

    val url = newsItem.originalUrl.orEmpty()

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
                        val webView = webViewRef
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(
                        enabled = url.isNotBlank(),
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, newsItem.title)
                                putExtra(Intent.EXTRA_TEXT, listOf(newsItem.title, url).joinToString("\n"))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享链接"))
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "分享",
                            tint = if (url.isNotBlank()) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = TextSecondary,
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
                .background(Bg)
                .padding(padding),
        ) {
            when (val state = articleState) {
                ArticleState.Loading -> {
                    isLoading = true
                    DetailSkeleton()
                }

                is ArticleState.WebUrl -> {
                    NewsWebView(
                        url = state.url,
                        html = null,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { webViewRef = it },
                        onProgress = { progress = it },
                        onTitle = { if (!it.isNullOrBlank() && !it.startsWith("http")) pageTitle = it },
                        onLoading = { isLoading = it },
                    )
                }

                is ArticleState.Html -> {
                    NewsWebView(
                        url = null,
                        html = state.html,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { webViewRef = it },
                        onProgress = { progress = it },
                        onLoading = { isLoading = it },
                    )
                }
            }

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = ToutiaoRed,
                    trackColor = Color.Transparent,
                )
            }

            AiDetailPanel(
                summaryState = aiSummaryState,
                chatState = aiChatState,
                question = question,
                onQuestionChange = { question = it },
                onAsk = {
                    viewModel.askAi(question)
                    question = ""
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun AiDetailPanel(
    summaryState: AiUiState<AiSummaryResponse>,
    chatState: AiUiState<AiChatResponse>,
    question: String,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = ToutiaoRed,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "AI 阅读助手",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))

        when (summaryState) {
            AiUiState.Idle, AiUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = ToutiaoRed,
                )
                Text(
                    text = "AI 正在提炼这篇新闻...",
                    color = TextCaption,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            is AiUiState.Error -> Text(
                text = summaryState.message,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            is AiUiState.Success -> {
                if (summaryState.data.summary.isNotBlank()) {
                    Text(
                        text = summaryState.data.summary,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                summaryState.data.bullets.take(2).forEach {
                    Text(
                        text = "• $it",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        if (chatState is AiUiState.Success && chatState.data.answer.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = chatState.data.answer,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (chatState is AiUiState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = chatState.message,
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = { Text("问问这篇新闻", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            )
            Button(
                onClick = onAsk,
                enabled = question.isNotBlank() && chatState !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = ToutiaoRed),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .height(48.dp),
            ) {
                if (chatState is AiUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NewsWebView(
    url: String?,
    html: String?,
    modifier: Modifier = Modifier,
    onReady: (WebView) -> Unit,
    onProgress: (Int) -> Unit,
    onTitle: ((String?) -> Unit)? = null,
    onLoading: (Boolean) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                settings.loadsImagesAutomatically = true
                settings.defaultTextEncodingName = "UTF-8"
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                setBackgroundColor(android.graphics.Color.WHITE)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, loadedUrl: String?, favicon: Bitmap?) {
                        onLoading(true)
                    }

                    override fun onPageFinished(view: WebView?, loadedUrl: String?) {
                        onLoading(false)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val requestUrl = request?.url?.toString().orEmpty()
                        if (requestUrl.startsWith("http://") || requestUrl.startsWith("https://")) {
                            view?.loadUrl(requestUrl)
                        }
                        return true
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        onTitle?.invoke(title)
                    }
                }

                onReady(this)
                when {
                    !url.isNullOrBlank() -> loadUrl(url)
                    !html.isNullOrBlank() -> loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
                }
            }
        },
        update = { webView ->
            onReady(webView)
        },
    )
}

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
    val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(ShimmerBase, ShimmerHighlight, ShimmerBase),
        start = androidx.compose.ui.geometry.Offset(translateAnim.value - 300f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim.value, 0f),
    )

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp),
    ) {
        SkeletonBlock(shimmerBrush, Modifier.fillMaxWidth().height(22.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        SkeletonBlock(shimmerBrush, Modifier.fillMaxWidth(0.75f).height(22.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        SkeletonBlock(shimmerBrush, Modifier.size(width = 100.dp, height = 14.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))

        repeat(5) { index ->
            SkeletonBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .fillMaxWidth(if (index == 4) 0.6f else 1f)
                    .height(14.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        SkeletonBlock(
            brush = shimmerBrush,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color.Transparent),
        )
    }
}

@Composable
private fun SkeletonBlock(brush: androidx.compose.ui.graphics.Brush, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(brush),
    )
}
