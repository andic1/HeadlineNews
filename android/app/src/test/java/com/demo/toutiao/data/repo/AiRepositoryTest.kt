package com.demo.toutiao.data.repo

import com.demo.toutiao.data.api.AiApi
import com.demo.toutiao.data.api.AiChatRequest
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiDailyBriefRequest
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.api.AiSummaryRequest
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AiRepositoryTest {
    @Test
    fun `daily brief only sends first fifteen items`() = runTest {
        val api = FakeAiApi()
        val repo = AiRepository(api)
        val items = (1..20).map { index ->
            NewsItem(
                id = index.toString(),
                category = "推荐",
                title = "新闻$index",
                description = "正文$index",
                source = "测试源",
                imageUrl = null,
                originalUrl = "https://example.com/$index",
                publishTime = "2026-05-31 12:00",
                layoutType = LayoutType.TEXT_ONLY,
            )
        }

        repo.dailyBrief(items)

        assertEquals(15, api.dailyBriefRequest?.items?.size)
        assertEquals("新闻1", api.dailyBriefRequest?.items?.first()?.title)
        assertEquals("新闻15", api.dailyBriefRequest?.items?.last()?.title)
    }

    @Test
    fun `news item maps into ai payload`() {
        val item = NewsItem(
            id = "1",
            category = "推荐",
            title = "标题",
            description = "正文",
            source = "来源",
            imageUrl = null,
            originalUrl = "https://example.com/news",
            publishTime = "2026-05-31 12:00",
            layoutType = LayoutType.TEXT_ONLY,
        )

        val payload = item.toAiPayload()

        assertEquals("标题", payload.title)
        assertEquals("来源", payload.source)
        assertEquals("https://example.com/news", payload.url)
        assertEquals("正文", payload.content)
    }
}

private class FakeAiApi : AiApi {
    var dailyBriefRequest: AiDailyBriefRequest? = null

    override suspend fun summarize(
        appToken: String,
        request: AiSummaryRequest,
    ): AiSummaryResponse = AiSummaryResponse()

    override suspend fun dailyBrief(
        appToken: String,
        request: AiDailyBriefRequest,
    ): AiDailyBriefResponse {
        dailyBriefRequest = request
        return AiDailyBriefResponse()
    }

    override suspend fun chat(
        appToken: String,
        request: AiChatRequest,
    ): AiChatResponse = AiChatResponse()
}
