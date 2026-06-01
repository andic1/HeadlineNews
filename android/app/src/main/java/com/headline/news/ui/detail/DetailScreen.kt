package com.headline.news.ui.detail

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.headline.news.data.api.AiSource
import com.headline.news.data.api.AiSummaryResponse
import com.headline.news.data.api.ArticleBlock
import com.headline.news.data.api.ArticleExtractResponse
import com.headline.news.data.model.NewsItem
import com.headline.news.ui.ai.AiChatMessage
import com.headline.news.ui.ai.AiChatRole
import com.headline.news.ui.ai.AiUiState
import com.headline.news.ui.theme.Bg
import com.headline.news.ui.theme.ShimmerBase
import com.headline.news.ui.theme.ShimmerHighlight
import com.headline.news.ui.theme.TextCaption
import com.headline.news.ui.theme.TextPrimary
import com.headline.news.ui.theme.TextSecondary
import com.headline.news.ui.theme.BrandRed

private const val TEXT_ORIGINAL_READING = "\u539f\u6587\u9605\u8bfb"
private const val TEXT_BACK = "\u8fd4\u56de"
private const val TEXT_SHARE = "\u5206\u4eab"
private const val TEXT_SHARE_LINK = "\u5206\u4eab\u94fe\u63a5"
private const val TEXT_MORE = "\u66f4\u591a"
private const val TEXT_AI_ASSISTANT = "AI \u9605\u8bfb\u52a9\u624b"
private const val TEXT_NATIVE_READING = "\u5df2\u8f6c\u6210\u539f\u751f\u9605\u8bfb\uff0c\u6b63\u6587\u52a0\u8f7d\u66f4\u5feb"
private const val TEXT_AI_LOADING = "\u6b63\u5728\u7406\u89e3\u8fd9\u7bc7\u65b0\u95fb..."
private const val TEXT_AI_RETRY = "AI \u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u70b9\u5f00\u53ef\u91cd\u8bd5"
private const val TEXT_AI_OPEN = "\u70b9\u5f00\u67e5\u770b\u901f\u8bfb\u548c\u7ee7\u7eed\u8ffd\u95ee"
private const val TEXT_COLLAPSE = "\u6536\u8d77"
private const val TEXT_CLOSE = "\u5173\u95ed"
private const val TEXT_EXPAND = "\u5c55\u5f00"
private const val TEXT_SUMMARY_LOADING = "AI \u6b63\u5728\u63d0\u70bc\u8fd9\u7bc7\u65b0\u95fb..."
private const val TEXT_RETRY = "\u91cd\u8bd5"
private const val TEXT_THINKING = "AI \u6b63\u5728\u601d\u8003..."
private const val TEXT_ASK_PLACEHOLDER = "\u95ee\u95ee\u8fd9\u7bc7\u65b0\u95fb"
private const val TEXT_SEND = "\u53d1\u9001"
private const val TEXT_REFERENCE = "\u53c2\u8003\u6765\u6e90"
private const val TEXT_QUICK_SUMMARY = "\u4e00\u53e5\u8bdd\u8bb2\u6e05\u695a"
private const val TEXT_QUICK_IMPACT = "\u5f71\u54cd\u662f\u4ec0\u4e48"
private const val TEXT_QUICK_FOCUS = "\u6211\u8be5\u5173\u6ce8\u4ec0\u4e48"
private const val TEXT_OPEN_WEB_FALLBACK = "\u539f\u751f\u89e3\u6790\u5931\u8d25\uff0c\u5df2\u4f7f\u7528\u539f\u7f51\u9875\u515c\u5e95"

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
    val aiMessages by viewModel.messages.collectAsState()

    var isWebLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(newsItem.title.ifBlank { newsItem.source ?: TEXT_ORIGINAL_READING }) }
    var question by remember { mutableStateOf("") }
    var aiPanelState by remember(newsItem.id) { mutableStateOf(AiAssistantPanelState.Collapsed) }

    fun expandAiPanel() {
        viewModel.ensureAiSummary()
        aiPanelState = AiAssistantPanelState.Expanded
    }

    LaunchedEffect(newsItem.id) {
        viewModel.loadArticle(newsItem)
        pageTitle = newsItem.title.ifBlank { newsItem.source ?: TEXT_ORIGINAL_READING }
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
                            contentDescription = TEXT_BACK,
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
                            context.startActivity(Intent.createChooser(shareIntent, TEXT_SHARE_LINK))
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = TEXT_SHARE,
                            tint = if (url.isNotBlank()) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = TEXT_MORE,
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
                ArticleState.Loading -> DetailSkeleton()

                is ArticleState.NativeArticle -> NativeArticleView(
                    article = state.article,
                    modifier = Modifier.fillMaxSize(),
                    onTitle = { if (it.isNotBlank()) pageTitle = it },
                )

                is ArticleState.WebUrl -> {
                    NewsWebView(
                        url = state.url,
                        modifier = Modifier.fillMaxSize(),
                        onReady = { webViewRef = it },
                        onProgress = { progress = it },
                        onTitle = { if (!it.isNullOrBlank() && !it.startsWith("http")) pageTitle = it },
                        onLoading = { isWebLoading = it },
                    )
                    Text(
                        text = TEXT_OPEN_WEB_FALLBACK,
                        color = TextCaption,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = articleState is ArticleState.WebUrl && isWebLoading,
                enter = fadeIn(),
                exit = fadeOut(tween(300)),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = BrandRed,
                    trackColor = Color.Transparent,
                )
            }

            when (aiPanelState) {
                AiAssistantPanelState.Closed -> AiAssistantReopenButton(
                    onOpen = { aiPanelState = AiAssistantPanelState.Collapsed },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )

                AiAssistantPanelState.Collapsed -> AiAssistantCollapsedBar(
                    summaryState = aiSummaryState,
                    onExpand = { expandAiPanel() },
                    onClose = { aiPanelState = AiAssistantPanelState.Closed },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                AiAssistantPanelState.Expanded -> AiDetailPanel(
                    summaryState = aiSummaryState,
                    chatState = aiChatState,
                    messages = aiMessages,
                    question = question,
                    onQuestionChange = { question = it },
                    onAsk = {
                        viewModel.askAi(question)
                        question = ""
                    },
                    onAskQuick = { quickQuestion ->
                        viewModel.askAi(quickQuestion)
                    },
                    onRetrySummary = { viewModel.ensureAiSummary(force = true) },
                    onCollapse = { aiPanelState = AiAssistantPanelState.Collapsed },
                    onClose = { aiPanelState = AiAssistantPanelState.Closed },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

private enum class AiAssistantPanelState {
    Collapsed,
    Expanded,
    Closed,
}

@Composable
private fun NativeArticleView(
    article: ArticleExtractResponse,
    modifier: Modifier = Modifier,
    onTitle: (String) -> Unit,
) {
    LaunchedEffect(article.title) {
        onTitle(article.title)
    }
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .background(Color.White)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 128.dp),
    ) {
        Text(
            text = article.title,
            color = TextPrimary,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!article.source.isNullOrBlank()) {
                Text(article.source.orEmpty(), color = BrandRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (!article.publishTime.isNullOrBlank()) {
                Text(article.publishTime.orEmpty(), color = TextCaption, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        article.blocks.forEach { block ->
            ArticleBlockView(block)
        }
    }
}

@Composable
private fun ArticleBlockView(block: ArticleBlock) {
    when (block.type) {
        "image" -> {
            val imageUrl = block.url.orEmpty()
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = block.alt,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
                if (!block.alt.isNullOrBlank()) {
                    Text(
                        text = block.alt,
                        color = TextCaption,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
        }

        else -> {
            val text = block.text.orEmpty()
            if (text.isNotBlank()) {
                Text(
                    text = text,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    lineHeight = 31.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AiAssistantReopenButton(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(14.dp)
            .background(BrandRed, RoundedCornerShape(999.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "AI",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun AiAssistantCollapsedBar(
    summaryState: AiUiState<AiSummaryResponse>,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(22.dp))
            .clickable(onClick = onExpand)
            .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(TEXT_AI_ASSISTANT, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                text = when (summaryState) {
                    AiUiState.Idle -> TEXT_NATIVE_READING
                    AiUiState.Loading -> TEXT_AI_LOADING
                    is AiUiState.Error -> TEXT_AI_RETRY
                    is AiUiState.Success -> summaryState.data.summary.ifBlank { TEXT_AI_OPEN }
                },
                color = TextCaption,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onExpand) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = TEXT_EXPAND, tint = TextSecondary)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = TEXT_CLOSE, tint = TextSecondary)
        }
    }
}

@Composable
private fun AiDetailPanel(
    summaryState: AiUiState<AiSummaryResponse>,
    chatState: AiUiState<String>,
    messages: List<AiChatMessage>,
    question: String,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    onAskQuick: (String) -> Unit,
    onRetrySummary: () -> Unit,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentScrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.68f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .background(Color.White.copy(alpha = 0.96f), RoundedCornerShape(24.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = TEXT_AI_ASSISTANT,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCollapse) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = TEXT_COLLAPSE, tint = TextSecondary)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = TEXT_CLOSE, tint = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(contentScrollState),
        ) {
            when (summaryState) {
                AiUiState.Idle, AiUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = BrandRed,
                    )
                    Text(
                        text = TEXT_SUMMARY_LOADING,
                        color = TextCaption,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                is AiUiState.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = summaryState.message,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRetrySummary) {
                        Text(TEXT_RETRY, color = BrandRed, fontSize = 12.sp)
                    }
                }

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
                            text = "\u2022 $it",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (messages.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                messages.takeLast(8).forEach { message ->
                    AiMessageBubble(message)
                    Spacer(Modifier.height(6.dp))
                }
            } else if (chatState is AiUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = chatState.message,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }

            if (chatState is AiUiState.Loading) {
                Spacer(Modifier.height(8.dp))
                Text(TEXT_THINKING, color = TextCaption, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        QuickQuestionRow(
            enabled = chatState !is AiUiState.Loading,
            onAskQuestion = onAskQuick,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = { Text(TEXT_ASK_PLACEHOLDER, fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp),
            )
            Button(
                onClick = onAsk,
                enabled = question.isNotBlank() && chatState !is AiUiState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
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
                        contentDescription = TEXT_SEND,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiChatMessage) {
    val isUser = message.role == AiChatRole.User
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .background(
                    color = if (isUser) BrandRed else Color(0xFFF5F7FA),
                    shape = RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 14.dp,
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.text,
                color = if (isUser) Color.White else TextPrimary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            if (!isUser && message.sources.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                message.sources.take(3).forEach { source ->
                    SourceChip(
                        source = source,
                        onClick = {
                            if (source.url.isNotBlank()) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
                            }
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun SourceChip(source: AiSource, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .clickable(enabled = source.url.isNotBlank(), onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = source.title.ifBlank { TEXT_REFERENCE },
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (source.snippet.isNotBlank()) {
            Text(
                text = source.snippet,
                color = TextCaption,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (source.url.isNotBlank()) {
            Text(
                text = source.url,
                color = BrandRed,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun QuickQuestionRow(enabled: Boolean, onAskQuestion: (String) -> Unit) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(TEXT_QUICK_SUMMARY, TEXT_QUICK_IMPACT, TEXT_QUICK_FOCUS).forEach { text ->
            TextButton(
                enabled = enabled,
                onClick = { onAskQuestion(text) },
            ) {
                Text(text, color = BrandRed, fontSize = 12.sp)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NewsWebView(
    url: String,
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
                loadUrl(url)
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
