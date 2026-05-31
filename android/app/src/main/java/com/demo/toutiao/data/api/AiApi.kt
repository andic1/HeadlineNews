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

    @POST("/api/ai/chat")
    suspend fun chat(
        @Header("X-App-Token") appToken: String,
        @Body request: AiChatRequest,
    ): AiChatResponse
}

@Serializable
data class AiNewsPayload(
    val title: String,
    val source: String? = null,
    val url: String? = null,
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
data class AiDailyBriefResponse(
    val title: String = "",
    val items: List<AiBriefItem> = emptyList(),
    val provider: String = "",
    val model: String = "",
    val cached: Boolean = false,
)

@Serializable
data class AiBriefItem(
    val title: String = "",
    val reason: String = "",
)

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
