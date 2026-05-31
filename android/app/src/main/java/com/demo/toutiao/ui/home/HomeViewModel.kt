package com.demo.toutiao.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.demo.toutiao.data.api.AiDailyBriefResponse
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.repo.AiRepository
import com.demo.toutiao.data.repo.NewsRepository
import com.demo.toutiao.ui.ai.AiUiState
import com.demo.toutiao.ui.ai.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: NewsRepository,
    private val aiRepo: AiRepository,
) : ViewModel() {

    val categories: List<String> = Categories.ALL

    /** 每个 category 一份 PagingFlow，懒初始化、缓存到 ViewModel scope */
    private val flows: MutableMap<String, Flow<PagingData<NewsItem>>> = mutableMapOf()

    fun pagingFlow(category: String): Flow<PagingData<NewsItem>> =
        flows.getOrPut(category) {
            repo.pagingFlow(category).cachedIn(viewModelScope)
        }

    /** 当前选中要查看详情的新闻 */
    private val _selectedItem = MutableStateFlow<NewsItem?>(null)
    val selectedItem: StateFlow<NewsItem?> = _selectedItem

    private val _dailyBriefState =
        MutableStateFlow<AiUiState<AiDailyBriefResponse>>(AiUiState.Idle)
    val dailyBriefState: StateFlow<AiUiState<AiDailyBriefResponse>> = _dailyBriefState

    private var lastBriefSignature: String? = null

    fun selectItem(item: NewsItem) {
        _selectedItem.value = item
    }

    fun loadDailyBrief(items: List<NewsItem>) {
        val candidates = items.take(15).filter { it.title.isNotBlank() }
        if (candidates.size < 5) return

        val signature = candidates.joinToString("|") { it.id }
        if (signature == lastBriefSignature) return
        lastBriefSignature = signature

        viewModelScope.launch {
            _dailyBriefState.value = AiUiState.Loading
            _dailyBriefState.value = runCatching {
                aiRepo.dailyBrief(candidates)
            }.fold(
                onSuccess = { AiUiState.Success(it) },
                onFailure = { AiUiState.Error(it.toUserMessage()) },
            )
        }
    }
}
