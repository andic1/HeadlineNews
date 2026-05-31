package com.demo.toutiao.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.demo.toutiao.data.model.Categories
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.Bg
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    items: LazyPagingItems<NewsItem>,
    category: String,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            items.refresh()
        }
    }

    LaunchedEffect(items.loadState.refresh) {
        if (items.loadState.refresh !is LoadState.Loading) {
            pullRefreshState.endRefresh()
        }
    }

    Box(modifier = modifier.nestedScroll(pullRefreshState.nestedScrollConnection)) {
        val refreshState = items.loadState.refresh

        when {
            refreshState is LoadState.Loading && items.itemCount == 0 -> FullScreenLoading()

            refreshState is LoadState.Error && items.itemCount == 0 -> {
                FullScreenError(
                    message = refreshState.error.message ?: "网络异常，请稍后重试",
                    onRetry = { items.retry() },
                )
            }

            items.itemCount == 0 -> FullScreenEmpty()

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Bg),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp),
                ) {
                    if (header != null) {
                        item(key = "news-list-header") {
                            header()
                        }
                    }

                    items(
                        count = items.itemCount,
                        key = { index -> items.peek(index)?.let { "${it.category}-${it.id}" } ?: index },
                    ) { index ->
                        items[index]?.let { item ->
                            NewsCard(
                                item = item,
                                legacyStyle = category == Categories.TRENDING,
                                onNewsClick = onNewsClick,
                            )
                        }
                    }

                    item {
                        AppendFooter(state = items.loadState.append, onRetry = items::retry)
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Bg,
            contentColor = ToutiaoRed,
        )
    }
}

@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = ToutiaoRed,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun FullScreenError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("加载失败", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextCaption, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onRetry) {
            Text("点击重试", color = ToutiaoRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FullScreenEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无内容", color = TextCaption, fontSize = 14.sp)
    }
}

@Composable
private fun AppendFooter(state: LoadState, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(
                color = ToutiaoRed,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )

            is LoadState.Error -> TextButton(onClick = onRetry) {
                Text("加载失败，点此重试", color = TextSecondary, fontSize = 13.sp)
            }

            is LoadState.NotLoading -> {
                if (state.endOfPaginationReached) {
                    Text("没有更多内容了", color = TextCaption, fontSize = 12.sp)
                } else {
                    Text("加载更多", color = TextCaption, fontSize = 12.sp)
                }
            }
        }
    }
}
