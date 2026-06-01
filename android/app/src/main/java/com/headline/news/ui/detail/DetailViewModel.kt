package com.headline.news.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.headline.news.data.api.AiSummaryResponse
import com.headline.news.data.api.ArticleBlock
import com.headline.news.data.api.ArticleExtractResponse
import com.headline.news.data.model.NewsItem
import com.headline.news.data.model.displayPublishTime
import com.headline.news.data.repo.AiRepository
import com.headline.news.ui.ai.AiChatMessage
import com.headline.news.ui.ai.AiChatRole
import com.headline.news.ui.ai.AiUiState
import com.headline.news.ui.ai.toApiTurns
import com.headline.news.ui.ai.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ArticleState {
    data object Loading : ArticleState()
    data class NativeArticle(val article: ArticleExtractResponse) : ArticleState()
    data class WebUrl(val url: String) : ArticleState()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val aiRepo: AiRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ArticleState>(ArticleState.Loading)
    val state: StateFlow<ArticleState> = _state

    private val _summaryState = MutableStateFlow<AiUiState<AiSummaryResponse>>(AiUiState.Idle)
    val summaryState: StateFlow<AiUiState<AiSummaryResponse>> = _summaryState

    private val _chatState = MutableStateFlow<AiUiState<String>>(AiUiState.Idle)
    val chatState: StateFlow<AiUiState<String>> = _chatState

    private val _messages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val messages: StateFlow<List<AiChatMessage>> = _messages

    private var originalItem: NewsItem? = null
    private var aiItem: NewsItem? = null
    private var summaryItemId: String? = null
    private var summaryJob: Job? = null
    private var articleJob: Job? = null

    fun loadArticle(newsItem: NewsItem) {
        originalItem = newsItem
        aiItem = newsItem
        _messages.value = emptyList()
        _chatState.value = AiUiState.Idle
        _summaryState.value = AiUiState.Idle
        summaryItemId = null
        summaryJob?.cancel()
        articleJob?.cancel()

        val url = newsItem.originalUrl.orEmpty().trim()
        if (url.isBlank()) {
            val localArticle = buildLocalArticle(newsItem)
            _state.value = ArticleState.NativeArticle(localArticle)
            aiItem = newsItem.withExtractedArticle(localArticle)
            return
        }

        _state.value = ArticleState.Loading
        articleJob = viewModelScope.launch {
            val extracted = runCatching { aiRepo.extractArticle(newsItem) }.getOrNull()
            if (extracted != null && extracted.success && extracted.blocks.isNotEmpty()) {
                _state.value = ArticleState.NativeArticle(extracted)
                aiItem = newsItem.withExtractedArticle(extracted)
            } else {
                _state.value = ArticleState.WebUrl(url)
            }
        }
    }

    fun ensureAiSummary(force: Boolean = false) {
        val item = aiItem ?: originalItem ?: return
        val state = _summaryState.value
        if (state is AiUiState.Loading) return
        if (!force && summaryItemId == item.id && state is AiUiState.Success) return
        loadAiSummary(item)
    }

    fun askAi(question: String) {
        val item = aiItem ?: originalItem ?: return
        val cleanQuestion = question.trim()
        if (cleanQuestion.isBlank()) return

        val previousMessages = _messages.value
        _messages.value = previousMessages + AiChatMessage(AiChatRole.User, cleanQuestion)

        viewModelScope.launch {
            _chatState.value = AiUiState.Loading
            _chatState.value = runCatching {
                aiRepo.chatMessage(
                    item = item,
                    question = cleanQuestion,
                    history = previousMessages.toApiTurns(),
                )
            }.fold(
                onSuccess = { response ->
                    if (response.answer.isNotBlank()) {
                        _messages.value = _messages.value + AiChatMessage(
                            role = AiChatRole.Assistant,
                            text = response.answer,
                            sources = response.sources,
                        )
                    }
                    AiUiState.Success(response.answer)
                },
                onFailure = { error ->
                    AiUiState.Error(error.toUserMessage())
                },
            )
        }
    }

    private fun loadAiSummary(newsItem: NewsItem) {
        summaryJob?.cancel()
        summaryItemId = newsItem.id
        summaryJob = viewModelScope.launch {
            _summaryState.value = AiUiState.Loading
            _summaryState.value = runCatching {
                aiRepo.summarize(newsItem)
            }.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.toUserMessage()) },
            )
        }
    }

    private fun buildLocalArticle(newsItem: NewsItem): ArticleExtractResponse {
        val blocks = mutableListOf<ArticleBlock>()
        val imageUrl = newsItem.imageUrl.orEmpty().trim()
        val description = newsItem.description.orEmpty().trim()
        if (imageUrl.isNotBlank()) {
            blocks += ArticleBlock(type = "image", url = imageUrl, alt = newsItem.title)
        }
        description
            .split(Regex("\\n+|(?<=[。！？])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { blocks += ArticleBlock(type = "text", text = it) }

        return ArticleExtractResponse(
            success = blocks.isNotEmpty(),
            title = newsItem.title,
            source = newsItem.source,
            publishTime = newsItem.displayPublishTime() ?: newsItem.publishTime,
            url = newsItem.originalUrl,
            imageUrl = newsItem.imageUrl,
            blocks = blocks,
        )
    }

    private fun NewsItem.withExtractedArticle(article: ArticleExtractResponse): NewsItem {
        val content = article.blocks
            .filter { it.type == "text" }
            .mapNotNull { it.text?.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .take(6000)
            .ifBlank { description.orEmpty() }
        return copy(
            title = article.title.ifBlank { title },
            description = content,
            source = article.source ?: source,
            imageUrl = article.imageUrl ?: imageUrl,
            originalUrl = article.url ?: originalUrl,
            publishTime = article.publishTime ?: publishTime,
        )
    }
}
