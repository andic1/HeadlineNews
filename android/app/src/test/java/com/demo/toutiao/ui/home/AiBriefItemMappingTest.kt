package com.demo.toutiao.ui.home

import com.demo.toutiao.data.api.AiBriefItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AiBriefItemMappingTest {
    @Test
    fun `ai brief item maps to clickable fallback news item`() {
        val item = AiBriefItem(
            newsId = "ai-1",
            title = "AI 推荐新闻",
            reason = "值得关注",
            url = "https://example.com/news",
            source = "今日头条",
            publishTime = "2026-05-31 15:44",
            imageUrl = "https://example.com/cover.jpg",
        )

        val news = item.toFallbackNewsItem(category = "推荐")

        assertEquals("ai-1", news.id)
        assertEquals("推荐", news.category)
        assertEquals("AI 推荐新闻", news.title)
        assertEquals("值得关注", news.description)
        assertEquals("https://example.com/news", news.originalUrl)
        assertEquals("https://example.com/cover.jpg", news.imageUrl)
    }
}
