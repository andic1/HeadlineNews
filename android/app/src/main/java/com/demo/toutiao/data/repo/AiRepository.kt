package com.demo.toutiao.data.repo

import com.demo.toutiao.BuildConfig
import com.demo.toutiao.data.api.AiApi
import com.demo.toutiao.data.api.AiChatRequest
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiDailyBriefRequest
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.api.AiNewsPayload
import com.demo.toutiao.data.api.AiSummaryRequest
import com.demo.toutiao.data.api.AiSummaryResponse
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.model.displayPublishTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val api: AiApi,
) {
    suspend fun summarize(item: NewsItem): AiSummaryResponse {
        return api.summarize(BuildConfig.AI_APP_TOKEN, AiSummaryRequest(item.toAiPayload()))
    }

    suspend fun dailyBrief(items: List<NewsItem>): AiDailyBriefResponse {
        return api.dailyBrief(
            BuildConfig.AI_APP_TOKEN,
            AiDailyBriefRequest(items.take(15).map { it.toAiPayload() }),
        )
    }

    suspend fun chat(item: NewsItem, question: String): AiChatResponse {
        return api.chat(
            BuildConfig.AI_APP_TOKEN,
            AiChatRequest(news = item.toAiPayload(maxDescriptionLength = 6000), question = question),
        )
    }
}

fun NewsItem.toAiPayload(maxDescriptionLength: Int = 3000): AiNewsPayload {
    val descriptionText = description
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.take(maxDescriptionLength)

    return AiNewsPayload(
        title = title,
        source = source,
        url = originalUrl,
        publishTime = displayPublishTime() ?: publishTime,
        content = descriptionText,
    )
}
