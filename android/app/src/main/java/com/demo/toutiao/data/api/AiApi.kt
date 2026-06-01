package com.demo.toutiao.data.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AiApi {
    @POST("/api/ai/summary")
    suspend fun summarize(
        @Header("X-App-Token") appToken: String,
        @Body request: AiSummaryRequest,
    ): AiSummaryResponse

    @POST("/api/ai/daily-brief")
    suspend fun dailyBrief(
        @Header("X-App-Token") appToken: String,
        @Body request: AiDailyBriefRequest,
    ): AiDailyBriefResponse

    @POST("/api/ai/news-rank")
    suspend fun newsRank(
        @Header("X-App-Token") appToken: String,
        @Body request: AiNewsRankRequest,
    ): AiNewsRankResponse

    @POST("/api/ai/chat")
    suspend fun chat(
        @Header("X-App-Token") appToken: String,
        @Body request: AiChatRequest,
    ): AiChatResponse

    @POST("/api/ai/chat/message")
    suspend fun chatMessage(
        @Header("X-App-Token") appToken: String,
        @Body request: AiChatMessageRequest,
    ): AiChatMessageResponse

    @POST("/api/article/extract")
    suspend fun extractArticle(
        @Header("X-App-Token") appToken: String,
        @Body request: ArticleExtractRequest,
    ): ArticleExtractResponse
}

@Serializable
data class AiNewsPayload(
    val id: String? = null,
    val category: String? = null,
    val title: String,
    val source: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
    val publishTime: String? = null,
    val content: String? = null,
)

@Serializable
data class AiSummaryRequest(
    val news: AiNewsPayload,
)

@Serializable
data class AiSummaryResponse(
    val summary: String = "",
    val bullets: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val provider: String = "",
    val model: String = "",
    val cached: Boolean = false,
)

@Serializable
data class AiDailyBriefRequest(
    val items: List<AiNewsPayload>,
)

@Serializable
data class AiNewsRankRequest(
    val items: List<AiNewsPayload>,
    val maxItems: Int = 5,
)

@Serializable
data class AiDailyBriefResponse(
    val title: String = "",
    val items: List<AiBriefItem> = emptyList(),
    val provider: String = "",
    val model: String = "",
    val cached: Boolean = false,
)

@Serializable
data class AiBriefItem(
    val newsId: String = "",
    val title: String = "",
    val reason: String = "",
    val url: String? = null,
    val source: String? = null,
    val publishTime: String? = null,
    val imageUrl: String? = null,
)

typealias AiNewsRankResponse = AiDailyBriefResponse

@Serializable
data class AiChatRequest(
    val news: AiNewsPayload,
    val question: String,
)

@Serializable
data class AiChatResponse(
    val answer: String = "",
    val provider: String = "",
    val model: String = "",
)

@Serializable
data class AiChatTurn(
    val role: String,
    val content: String,
)

@Serializable
data class AiChatMessageRequest(
    val news: AiNewsPayload,
    val question: String,
    val history: List<AiChatTurn> = emptyList(),
)

@Serializable
data class AiChatMessageResponse(
    val answer: String = "",
    val suggestedQuestions: List<String> = emptyList(),
    val sources: List<AiSource> = emptyList(),
    val provider: String = "",
    val model: String = "",
    val cached: Boolean = false,
)

@Serializable
data class AiSource(
    val title: String = "",
    val url: String = "",
    val snippet: String = "",
)

@Serializable
data class ArticleExtractRequest(
    val news: AiNewsPayload,
)

@Serializable
data class ArticleExtractResponse(
    val success: Boolean = false,
    val title: String = "",
    val source: String? = null,
    val publishTime: String? = null,
    val url: String? = null,
    val imageUrl: String? = null,
    val blocks: List<ArticleBlock> = emptyList(),
    val cached: Boolean = false,
)

@Serializable
data class ArticleBlock(
    val type: String = "text",
    val text: String? = null,
    val url: String? = null,
    val alt: String? = null,
)
