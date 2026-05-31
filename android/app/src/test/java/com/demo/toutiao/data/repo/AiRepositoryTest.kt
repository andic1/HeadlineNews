package com.demo.toutiao.data.repo

import com.demo.toutiao.data.api.AiApi
import com.demo.toutiao.data.api.AiChatRequest
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiChatMessageRequest
import com.demo.toutiao.data.api.AiChatMessageResponse
import com.demo.toutiao.data.api.AiDailyBriefRequest
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.api.AiNewsRankRequest
import com.demo.toutiao.data.api.AiNewsRankResponse
import com.demo.toutiao.data.api.AiSummaryRequest
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AiRepositoryTest {
    @Test
    fun `news rank only sends first fifty items`() = runTest {
        val api = FakeAiApi()
        val repo = AiRepository(api)
        val items = (1..60).map { index ->
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

        assertEquals(50, api.newsRankRequest?.items?.size)
        assertEquals("新闻1", api.newsRankRequest?.items?.first()?.title)
        assertEquals("新闻50", api.newsRankRequest?.items?.last()?.title)
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
        assertEquals("1", payload.id)
        assertEquals("来源", payload.source)
        assertEquals("https://example.com/news", payload.url)
        assertEquals("正文", payload.content)
    }

    @Test
    fun `news rank sends clickable news payloads`() = runTest {
        val api = FakeAiApi()
        val repo = AiRepository(api)
        val item = NewsItem(
            id = "rank-1",
            category = "推荐",
            title = "值得看的新闻",
            description = "新闻正文",
            source = "今日头条",
            imageUrl = "https://example.com/cover.jpg",
            originalUrl = "https://example.com/news",
            publishTime = "2026-05-31 12:00",
            layoutType = LayoutType.TEXT_WITH_THUMB,
        )

        repo.dailyBrief(listOf(item))

        val payload = api.newsRankRequest?.items?.single()
        assertEquals("rank-1", payload?.id)
        assertEquals("推荐", payload?.category)
        assertEquals("https://example.com/news", payload?.url)
        assertEquals("https://example.com/cover.jpg", payload?.imageUrl)
    }
}

private class FakeAiApi : AiApi {
    var dailyBriefRequest: AiDailyBriefRequest? = null
    var newsRankRequest: AiNewsRankRequest? = null

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

    override suspend fun newsRank(
        appToken: String,
        request: AiNewsRankRequest,
    ): AiNewsRankResponse {
        newsRankRequest = request
        return AiNewsRankResponse()
    }

    override suspend fun chat(
        appToken: String,
        request: AiChatRequest,
    ): AiChatResponse = AiChatResponse()

    override suspend fun chatMessage(
        appToken: String,
        request: AiChatMessageRequest,
    ): AiChatMessageResponse = AiChatMessageResponse()
}
