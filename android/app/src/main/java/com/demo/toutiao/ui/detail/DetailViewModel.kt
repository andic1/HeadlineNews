package com.demo.toutiao.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.repo.AiRepository
import com.demo.toutiao.ui.ai.AiUiState
import com.demo.toutiao.ui.ai.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ArticleState {
    data object Loading : ArticleState()
    data class WebUrl(val url: String) : ArticleState()
    data class Html(val html: String) : ArticleState()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val aiRepo: AiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state

    private val _summaryState = MutableStateFlow<AiUiState<AiSummaryResponse>>(AiUiState.Idle)
    val summaryState: StateFlow<AiUiState<AiSummaryResponse>> = _summaryState

    private val _chatState = MutableStateFlow<AiUiState<AiChatResponse>>(AiUiState.Idle)
    val chatState: StateFlow<AiUiState<AiChatResponse>> = _chatState

    private var currentItem: NewsItem? = null

    fun loadArticle(newsItem: NewsItem) {
        currentItem = newsItem
        loadAiSummary(newsItem)

        val url = newsItem.originalUrl.orEmpty().trim()
        if (url.isNotBlank()) {
            _state.value = ArticleState.WebUrl(url)
            return
        }

        val description = newsItem.description.orEmpty().trim()
        val body = when {
            description.isNotBlank() -> "<p>${escapeHtml(description)}</p>"
            else -> "<p class='empty'>暂无详细内容</p>"
        }

        _state.value = ArticleState.Html(wrapInTemplate(newsItem, body))
    }

    fun askAi(question: String) {
        val item = currentItem ?: return
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) return

        viewModelScope.launch {
            _chatState.value = AiUiState.Loading
            _chatState.value = runCatching {
                aiRepo.chat(item, cleanQuestion)
            }.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.toUserMessage()) },
            )
        }
    }

    private fun loadAiSummary(newsItem: NewsItem) {
        viewModelScope.launch {
            _summaryState.value = AiUiState.Loading
            _summaryState.value = runCatching {
                aiRepo.summarize(newsItem)
            }.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.toUserMessage()) },
            )
        }
    }

    private fun wrapInTemplate(newsItem: NewsItem, bodyHtml: String): String {
        val source = newsItem.source.orEmpty()
        val time = newsItem.publishTime.orEmpty()
        val cover = newsItem.imageUrl.orEmpty()

        val coverSection = if (cover.isNotBlank()) {
            """<div class="cover"><img src="$cover" alt=""></div>"""
        } else {
            ""
        }

        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
                -webkit-tap-highlight-color: transparent;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB",
                             "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
                font-size: 17px;
                line-height: 1.8;
                color: #111827;
                background: #fff;
                padding: 0 20px 40px;
                -webkit-font-smoothing: antialiased;
                word-wrap: break-word;
                overflow-wrap: break-word;
            }
            .header {
                padding: 20px 0 16px;
                border-bottom: 1px solid #eef0f2;
                margin-bottom: 20px;
            }
            .title {
                font-size: 22px;
                font-weight: 700;
                line-height: 1.4;
                color: #111827;
                margin-bottom: 12px;
                letter-spacing: -0.3px;
            }
            .meta {
                display: flex;
                align-items: center;
                gap: 12px;
                font-size: 13px;
                color: #6b7280;
                flex-wrap: wrap;
            }
            .meta .source {
                color: #d63b30;
                font-weight: 600;
            }
            .cover {
                margin: 0 -20px 20px;
            }
            .cover img {
                width: 100%;
                height: auto;
                display: block;
            }
            .content p {
                margin: 16px 0;
                text-align: justify;
            }
            .content img {
                max-width: 100%;
                height: auto;
                border-radius: 8px;
                margin: 16px auto;
                display: block;
            }
            .content h2, .content h3, .content h4 {
                font-weight: 600;
                margin: 24px 0 12px;
                color: #111827;
            }
            .content h2 { font-size: 20px; }
            .content h3 { font-size: 18px; }
            .content h4 { font-size: 17px; }
            .content blockquote {
                border-left: 3px solid #d63b30;
                padding: 8px 16px;
                margin: 16px 0;
                background: #fafafa;
                color: #555;
                font-size: 15px;
            }
            .content a {
                color: #406599;
                text-decoration: none;
            }
            .content ul, .content ol {
                padding-left: 24px;
                margin: 12px 0;
            }
            .content li {
                margin: 6px 0;
            }
            .empty {
                text-align: center;
                color: #9ca3af;
                padding: 60px 0;
                font-size: 15px;
            }
            @media (prefers-color-scheme: dark) {
                body { background: #1a1a1a; color: #e5e7eb; }
                .header { border-bottom-color: #2f3338; }
                .title { color: #f3f4f6; }
                .content blockquote { background: #252525; color: #bbb; }
            }
            </style>
            </head>
            <body>
            <div class="header">
                <h1 class="title">${escapeHtml(newsItem.title)}</h1>
                <div class="meta">
                    ${if (source.isNotBlank()) """<span class="source">${escapeHtml(source)}</span>""" else ""}
                    ${if (time.isNotBlank()) """<span class="time">${escapeHtml(time)}</span>""" else ""}
                </div>
            </div>
            $coverSection
            <div class="content">
            $bodyHtml
            </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
