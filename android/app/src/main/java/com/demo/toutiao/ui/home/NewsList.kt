package com.demo.toutiao.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.Bg
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    items: LazyPagingItems<NewsItem>,
    modifier: Modifier = Modifier,
    onNewsClick: (title: String, source: String?, url: String) -> Unit = { _, _, _ -> },
) {
    val ptrState = rememberPullToRefreshState()
    if (ptrState.isRefreshing) {
        LaunchedEffect(true) {
            items.refresh()
        }
    }
    LaunchedEffect(items.loadState.refresh) {
        if (items.loadState.refresh !is LoadState.Loading) {
            ptrState.endRefresh()
        }
    }

    Box(modifier = modifier.nestedScroll(ptrState.nestedScrollConnection)) {
        val refreshState = items.loadState.refresh

        when {
            refreshState is LoadState.Loading && items.itemCount == 0 -> {
                FullScreenLoading()
            }
            refreshState is LoadState.Error && items.itemCount == 0 -> {
                FullScreenError(
                    message = refreshState.error.message ?: "加载失败",
                    onRetry = { items.retry() },
                )
            }
            items.itemCount == 0 -> {
                FullScreenEmpty()
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().background(Bg)) {
                    items(
                        count = items.itemCount,
                        key = { idx -> items.peek(idx)?.let { "${it.category}-${it.id}" } ?: idx },
                    ) { idx ->
                        items[idx]?.let { NewsCard(it, onNewsClick = onNewsClick) }
                    }
                    item {
                        AppendFooter(items.loadState.append) { items.retry() }
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = ptrState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ToutiaoRed)
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("加载失败：$message", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text("点击重试", color = ToutiaoRed)
        }
    }
}

@Composable
private fun FullScreenEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无内容", color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun AppendFooter(state: LoadState, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(
                color = ToutiaoRed,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )
            is LoadState.Error -> TextButton(onClick = onRetry) {
                Text("加载失败 点击重试", color = TextSecondary, fontSize = 13.sp)
            }
            is LoadState.NotLoading -> if (state.endOfPaginationReached) {
                Text("没有更多了", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}
