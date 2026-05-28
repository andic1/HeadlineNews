package com.demo.toutiao.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.demo.toutiao.data.article.ArticleExtractor
import com.demo.toutiao.data.model.NewsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ArticleState {
    data object Loading : ArticleState()
    data class Success(val html: String) : ArticleState()
    data object Fallback : ArticleState()  // 提取失败，降级到 WebView
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val extractor: ArticleExtractor,
) : ViewModel() {

    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state

    fun loadArticle(newsItem: NewsItem) {
        val url = newsItem.originalUrl
        if (url.isNullOrBlank()) {
            // 没有 URL，用描述生成简单 HTML
            val desc = newsItem.description ?: ""
            if (desc.isNotBlank()) {
                _state.value = ArticleState.Success(wrapInTemplate(newsItem, "<p>$desc</p>"))
            } else {
                _state.value = ArticleState.Success(wrapInTemplate(newsItem, "<p class='empty'>暂无详细内容</p>"))
            }
            return
        }

        viewModelScope.launch {
            _state.value = ArticleState.Loading
            val article = extractor.extract(url)
            if (article != null) {
                _state.value = ArticleState.Success(wrapInTemplate(newsItem, article.contentHtml))
            } else {
                // 提取失败：判断是否可降级到 WebView
                // 知乎、澎湃等有真实网页可以渲染；头条等 JS SPA 页面则用原生摘要
                val canFallbackToWebView = url.contains("zhihu.com")
                        || url.contains("thepaper.cn")
                        || url.contains("baidu.com")
                if (canFallbackToWebView) {
                    _state.value = ArticleState.Fallback
                } else {
                    // 生成原生摘要页面
                    val summaryHtml = buildSummaryHtml(newsItem)
                    _state.value = ArticleState.Success(wrapInTemplate(newsItem, summaryHtml))
                }
            }
        }
    }

    /**
     * 当正文提取失败时，生成一个原生摘要 HTML 页面
     */
    private fun buildSummaryHtml(newsItem: NewsItem): String {
        val sb = StringBuilder()
        // 显示描述（如果有）
        if (!newsItem.description.isNullOrBlank()) {
            sb.append("<p>${escapeHtml(newsItem.description)}</p>")
        }
        // 显示封面大图
        if (!newsItem.imageUrl.isNullOrBlank()) {
            sb.append("""<div style="margin:20px 0;"><img src="${newsItem.imageUrl}" style="width:100%;border-radius:8px;" alt=""></div>""")
        }
        // 如果什么内容都没有
        if (sb.isEmpty()) {
            sb.append("""<p style="color:#999;text-align:center;padding:40px 0;">暂无正文内容</p>""")
        }
        // 查看原文链接
        if (!newsItem.originalUrl.isNullOrBlank()) {
            sb.append("""
                <div style="margin-top:30px;padding-top:20px;border-top:1px solid #f0f0f0;text-align:center;">
                    <a href="${newsItem.originalUrl}" style="display:inline-block;padding:10px 24px;background:#d63b30;color:#fff;border-radius:20px;text-decoration:none;font-size:14px;font-weight:500;">查看原文</a>
                </div>
            """.trimIndent())
        }
        return sb.toString()
    }

    /**
     * 把正文 HTML 包装到精美的本地阅读模板里
     */
    private fun wrapInTemplate(newsItem: NewsItem, bodyHtml: String): String {
        val source = newsItem.source ?: ""
        val time = newsItem.publishTime ?: ""
        val cover = newsItem.imageUrl ?: ""

        val coverSection = if (cover.isNotBlank()) {
            """<div class="cover"><img src="$cover" alt=""></div>"""
        } else ""

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
    color: #222;
    background: #fff;
    padding: 0 20px 40px;
    -webkit-font-smoothing: antialiased;
    word-wrap: break-word;
    overflow-wrap: break-word;
}
.header {
    padding: 20px 0 16px;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 20px;
}
.title {
    font-size: 22px;
    font-weight: 700;
    line-height: 1.4;
    color: #111;
    margin-bottom: 12px;
    letter-spacing: -0.3px;
}
.meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 13px;
    color: #999;
}
.meta .source {
    color: #d63b30;
    font-weight: 500;
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
    border-radius: 6px;
    margin: 16px auto;
    display: block;
}
.content h2, .content h3, .content h4 {
    font-weight: 600;
    margin: 24px 0 12px;
    color: #111;
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
.content figure {
    margin: 16px 0;
    text-align: center;
}
.content figcaption {
    font-size: 13px;
    color: #999;
    margin-top: 6px;
}
.content table {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 14px;
}
.content th, .content td {
    border: 1px solid #eee;
    padding: 8px 12px;
    text-align: left;
}
.content th {
    background: #f7f7f7;
    font-weight: 600;
}
.empty {
    text-align: center;
    color: #ccc;
    padding: 60px 0;
    font-size: 15px;
}
/* 暗色模式，如果系统支持 */
@media (prefers-color-scheme: dark) {
    body { background: #1a1a1a; color: #e0e0e0; }
    .header { border-bottom-color: #333; }
    .title { color: #f0f0f0; }
    .content blockquote { background: #252525; color: #bbb; }
    .content th { background: #2a2a2a; }
    .content th, .content td { border-color: #333; }
}
</style>
</head>
<body>
<div class="header">
    <h1 class="title">${escapeHtml(newsItem.title)}</h1>
    <div class="meta">
        ${if (source.isNotBlank()) """<span class="source">$source</span>""" else ""}
        ${if (time.isNotBlank()) """<span class="time">$time</span>""" else ""}
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
