package com.demo.toutiao.data.repo

import com.demo.toutiao.BuildConfig
import com.demo.toutiao.data.api.AiApi
import com.demo.toutiao.data.api.AiChatRequest
import com.demo.toutiao.data.api.AiChatResponse
import com.demo.toutiao.data.api.AiChatMessageRequest
import com.demo.toutiao.data.api.AiChatMessageResponse
import com.demo.toutiao.data.api.AiChatTurn
import com.demo.toutiao.data.api.AiDailyBriefRequest
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.api.AiNewsPayload
import com.demo.toutiao.data.api.AiNewsRankRequest
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
        return api.newsRank(
            BuildConfig.AI_APP_TOKEN,
            AiNewsRankRequest(items.take(50).map { it.toAiPayload() }),
        )
    }

    suspend fun chat(item: NewsItem, question: String): AiChatResponse {
        return api.chat(
            BuildConfig.AI_APP_TOKEN,
            AiChatRequest(news = item.toAiPayload(maxDescriptionLength = 6000), question = question),
        )
    }

    suspend fun chatMessage(
        item: NewsItem,
        question: String,
        history: List<AiChatTurn>,
    ): AiChatMessageResponse {
        return api.chatMessage(
            BuildConfig.AI_APP_TOKEN,
            AiChatMessageRequest(
                news = item.toAiPayload(maxDescriptionLength = 6000),
                question = question,
                history = history.takeLast(8),
            ),
        )
    }
}

fun NewsItem.toAiPayload(maxDescriptionLength: Int = 3000): AiNewsPayload {
    val descriptionText = description
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.take(maxDescriptionLength)

    return AiNewsPayload(
        id = id,
        category = category,
        title = title,
        source = source,
        url = originalUrl,
        imageUrl = imageUrl,
        publishTime = displayPublishTime() ?: publishTime,
        content = descriptionText,
    )
}
