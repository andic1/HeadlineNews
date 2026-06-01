package com.demo.toutiao.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.Bg
import com.demo.toutiao.ui.theme.TopBarBg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val tabs = viewModel.categories
    val initialIndex = tabs.indexOf(Categories.DEFAULT).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { tabs.size }
    val scope = rememberCoroutineScope()
    val dailyBriefState by viewModel.dailyBriefState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.warmUpCategories()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(TopBarBg)) {
                HomeTopBar()
                CategoryTabRow(
                    tabs = tabs,
                    selectedIndex = pagerState.currentPage,
                    onSelect = { i -> scope.launch { pagerState.animateScrollToPage(i) } },
                )
            }
        },
        bottomBar = { BottomNavBar() },
        containerColor = Bg,
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Bg),
            key = { tabs[it] },
            beyondBoundsPageCount = tabs.lastIndex.coerceAtLeast(0),
        ) { pageIndex ->
            val category = tabs[pageIndex]
            val items = viewModel.pagingFlow(category).collectAsLazyPagingItems()
            val snapshotItems = items.itemSnapshotList.items
            val briefSignature = snapshotItems.take(15).joinToString("|") { it.id }

            LaunchedEffect(category, briefSignature, pagerState.currentPage) {
                if (pageIndex == pagerState.currentPage) {
                    viewModel.loadDailyBrief(snapshotItems)
                }
            }

            NewsList(
                items = items,
                category = category,
                modifier = Modifier.fillMaxSize(),
                header = {
                    if (pageIndex == pagerState.currentPage) {
                        AiBriefCard(
                            state = dailyBriefState,
                            onItemClick = { briefItem ->
                                val matched = snapshotItems.firstOrNull { item ->
                                    item.id == briefItem.newsId ||
                                        (!briefItem.url.isNullOrBlank() && item.originalUrl == briefItem.url)
                                }
                                onNewsClick(matched ?: briefItem.toFallbackNewsItem(category))
                            },
                        )
                    }
                },
                onRefresh = {
                    viewModel.refreshCategory(category)
                },
                onNewsClick = onNewsClick,
            )
        }
    }
}
