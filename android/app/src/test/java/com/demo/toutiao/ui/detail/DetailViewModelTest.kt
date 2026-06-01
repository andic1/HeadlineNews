package com.demo.toutiao.ui.detail

import com.demo.toutiao.data.api.AiApi
import com.demo.toutiao.data.api.AiChatMessageRequest
import com.demo.toutiao.data.api.AiChatMessageResponse
import com.demo.toutiao.data.api.AiChatRequest
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiDailyBriefRequest
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.api.AiNewsRankRequest
import com.demo.toutiao.data.api.AiSummaryRequest
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.api.ArticleExtractRequest
import com.demo.toutiao.data.api.ArticleExtractResponse
import com.demo.toutiao.data.model.LayoutType
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.repo.AiRepository
import com.demo.toutiao.ui.ai.AiUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `article load does not request ai summary until explicitly triggered`() = runTest(dispatcher) {
        val api = FakeAiApi()
        val viewModel = DetailViewModel(AiRepository(api))

        viewModel.loadArticle(sampleNews())
        advanceUntilIdle()

        assertEquals(0, api.summaryCalls)
        assertTrue(viewModel.summaryState.value is AiUiState.Idle)

        viewModel.ensureAiSummary()
        advanceUntilIdle()

        assertEquals(1, api.summaryCalls)
        assertTrue(viewModel.summaryState.value is AiUiState.Success)
    }

    private fun sampleNews(): NewsItem {
        return NewsItem(
            id = "news-1",
            category = "推荐",
            title = "测试新闻",
            description = "这是一段测试新闻正文",
            source = "测试源",
            imageUrl = null,
            originalUrl = "https://example.com/news",
            publishTime = "2026-05-31 12:00",
            layoutType = LayoutType.TEXT_ONLY,
        )
    }
}

private class FakeAiApi : AiApi {
    var summaryCalls = 0

    override suspend fun summarize(appToken: String, request: AiSummaryRequest): AiSummaryResponse {
        summaryCalls += 1
        return AiSummaryResponse(summary = "AI 摘要")
    }

    override suspend fun dailyBrief(appToken: String, request: AiDailyBriefRequest): AiDailyBriefResponse {
        return AiDailyBriefResponse()
    }

    override suspend fun newsRank(appToken: String, request: AiNewsRankRequest): AiDailyBriefResponse {
        return AiDailyBriefResponse()
    }

    override suspend fun chat(appToken: String, request: AiChatRequest): AiChatResponse {
        return AiChatResponse(answer = "回答")
    }

    override suspend fun chatMessage(appToken: String, request: AiChatMessageRequest): AiChatMessageResponse {
        return AiChatMessageResponse(answer = "回答")
    }

    override suspend fun extractArticle(appToken: String, request: ArticleExtractRequest): ArticleExtractResponse {
        return ArticleExtractResponse()
    }
}
