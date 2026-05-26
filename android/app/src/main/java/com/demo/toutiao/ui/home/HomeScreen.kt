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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.ui.theme.Bg
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNewsClick: (title: String, source: String?, url: String) -> Unit = { _, _, _ -> },
) {
    val tabs = viewModel.categories
    val initialIndex = tabs.indexOf(Categories.DEFAULT).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex) { tabs.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column {
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
        ) { pageIndex ->
            val category = tabs[pageIndex]
            val items = viewModel.pagingFlow(category).collectAsLazyPagingItems()
            NewsList(items = items, modifier = Modifier.fillMaxSize(), onNewsClick = onNewsClick)
        }
    }
}
