package com.headline.news.data.repo

import com.headline.news.BuildConfig
import com.headline.news.data.api.AiApi
import com.headline.news.data.api.AiChatRequest
import com.headline.news.data.api.AiChatResponse
import com.headline.news.data.api.AiChatMessageRequest
import com.headline.news.data.api.AiChatMessageResponse
import com.headline.news.data.api.AiChatTurn
import com.headline.news.data.api.AiDailyBriefRequest
import com.headline.news.data.api.AiDailyBriefResponse
import com.headline.news.data.api.AiNewsPayload
import com.headline.news.data.api.AiNewsRankRequest
import com.headline.news.data.api.AiSummaryRequest
import com.headline.news.data.api.AiSummaryResponse
import com.headline.news.data.api.ArticleExtractRequest
import com.headline.news.data.api.ArticleExtractResponse
import com.headline.news.data.model.NewsItem
import com.headline.news.data.model.displayPublishTime
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
            AiNewsRankRequest(
                items = items.take(15).map { it.toAiPayload() },
                maxItems = 3,
            ),
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

    suspend fun extractArticle(item: NewsItem): ArticleExtractResponse {
        return api.extractArticle(
            BuildConfig.AI_APP_TOKEN,
            ArticleExtractRequest(item.toAiPayload(maxDescriptionLength = 6000)),
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
