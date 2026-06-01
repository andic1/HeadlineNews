package com.headline.news.data.article

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从原文 URL 提取正文 HTML，返回干净的文章内容。
 */
@Singleton
class ArticleExtractor @Inject constructor(
    private val okHttp: OkHttpClient,
) {
    data class Article(
        val title: String,
        val contentHtml: String,
        val siteName: String?,
    )

    /**
     * 抓取 URL，提取正文。
     * 返回 null 表示提取失败，调用方可 fallback 到 WebView。
     */
    suspend fun extract(url: String): Article? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .build()

            val response = okHttp.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val html = response.body?.string() ?: return@withContext null
            val doc = Jsoup.parse(html, url)

            val title = extractTitle(doc)
            val bodyHtml = extractBody(doc, url)

            if (bodyHtml.isNullOrBlank() || bodyHtml.length < 100) {
                return@withContext null
            }

            val siteName = doc.select("meta[property=og:site_name]").attr("content")
                .ifBlank { null }

            Article(
                title = title ?: "",
                contentHtml = bodyHtml,
                siteName = siteName,
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTitle(doc: Document): String? {
        return doc.select("meta[property=og:title]").attr("content").ifBlank { null }
            ?: doc.select("h1").first()?.text()?.ifBlank { null }
            ?: doc.title().ifBlank { null }
    }

    private fun extractBody(doc: Document, url: String): String? {
        // 先移除干扰元素
        doc.select(
            "script, style, iframe, nav, header, footer, " +
            ".comment, .comments, .comment-list, .comment-area, " +
            ".recommend, .related, .sidebar, .ad, .advertisement, " +
            ".app-download, .download-bar, .open-app, .launch-app, " +
            ".share-bar, .toolbar, .action-bar, .bottom-bar, " +
            "[class*=download], [class*=openApp], [class*=open-app], " +
            "[class*=ad-wrapper], [class*=banner], [class*=recommend], " +
            ".article-tag, .article-footer, .feed-card, .related-news"
        ).remove()

        // 按优先级尝试提取正文
        val contentSelectors = getContentSelectors(url)
        for (selector in contentSelectors) {
            val el = doc.select(selector).first()
            if (el != null) {
                val html = el.html()
                if (html.length > 80) {
                    return cleanHtml(html, url)
                }
            }
        }
        return null
    }

    private fun getContentSelectors(url: String): List<String> {
        val platform = when {
            url.contains("zhihu.com") -> listOf(
                ".Post-RichTextContainer",
                ".RichContent-inner",
                ".Post-RichText",
                ".AnswerItem .RichContent",
                ".QuestionAnswer-content",
            )
            url.contains("toutiao.com") || url.contains("toutiao.cn") -> listOf(
                ".article-content",
                ".article__content",
                "#article-detail .content",
                ".syl-article-base",
            )
            url.contains("thepaper.cn") -> listOf(
                ".news_txt",
                ".index_cententWrap",
                ".news_part_father",
                ".newsdetail_content",
            )
            url.contains("v2ex.com") -> listOf(
                ".topic_content",
                ".markdown_body",
                "#Main .box",
                ".cell",
            )
            url.contains("geekpark.net") || url.contains("geekpark.cn") -> listOf(
                "article",
                ".article-content",
                ".post-content",
                ".content",
            )
            url.contains("tieba.baidu.com") -> listOf(
                ".p_postlist",
                ".d_post_content",
                ".mainContent",
            )
            url.contains("baidu.com") -> listOf(
                ".mainContent",
                "#article",
                ".article-content",
            )
            else -> emptyList()
        }
        // 通用 selector 放后面作为 fallback
        return platform + listOf(
            "article",
            "[itemprop=articleBody]",
            ".article-body",
            ".article-content",
            ".post-content",
            ".entry-content",
            ".content-body",
            ".story-body",
            "main .content",
            "#content",
            ".content",
            "main",
        )
    }

    private fun cleanHtml(html: String, baseUrl: String): String {
        // 允许基本格式标签和图片
        val safelist = Safelist.relaxed()
            .addTags("figure", "figcaption", "video", "source", "picture")
            .addAttributes("img", "src", "alt", "width", "height", "data-src", "data-original")
            .addAttributes("video", "src", "poster", "controls")
            .addAttributes("source", "src", "type")
            .addAttributes("a", "href")
            .addAttributes(":all", "class", "style")

        var cleaned = Jsoup.clean(html, baseUrl, safelist)

        // 处理懒加载图片：data-src / data-original → src
        val doc = Jsoup.parseBodyFragment(cleaned, baseUrl)
        doc.select("img").forEach { img ->
            val dataSrc = img.attr("data-src").ifBlank { img.attr("data-original") }
            if (dataSrc.isNotBlank() && img.attr("src").let { it.isBlank() || it.contains("data:image") || it.contains("placeholder") }) {
                img.attr("src", dataSrc)
            }
            // 移除 inline style 但保留自然尺寸
            img.removeAttr("style")
        }

        // 移除空段落
        doc.select("p").forEach { p ->
            if (p.text().isBlank() && p.select("img").isEmpty()) {
                p.remove()
            }
        }

        return doc.body().html()
    }
}
