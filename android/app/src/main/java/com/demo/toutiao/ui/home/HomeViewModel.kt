package com.demo.toutiao.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.data.repo.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: NewsRepository,
) : ViewModel() {

    val categories: List<String> = Categories.ALL

    /** 每个 category 一份 PagingFlow，懒初始化、缓存到 ViewModel scope */
    private val flows: MutableMap<String, Flow<PagingData<NewsItem>>> = mutableMapOf()

    fun pagingFlow(category: String): Flow<PagingData<NewsItem>> =
        flows.getOrPut(category) {
            repo.pagingFlow(category).cachedIn(viewModelScope)
        }
}
