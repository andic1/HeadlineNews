package com.headline.news.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.headline.news.data.model.NewsItem
import com.headline.news.ui.theme.Bg
import com.headline.news.ui.theme.CardBg
import com.headline.news.ui.theme.TextCaption
import com.headline.news.ui.theme.TextSecondary
import com.headline.news.ui.theme.BrandRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    items: LazyPagingItems<NewsItem>,
    category: String,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    onRefresh: suspend () -> Unit = {},
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            onRefresh()
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
                    message = refreshState.error.message ?: "\u7f51\u7edc\u5f02\u5e38\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5",
                    onRetry = { items.retry() },
                )
            }

            items.itemCount == 0 -> FullScreenEmpty()

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Bg),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
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
                                onNewsClick = onNewsClick,
                            )
                        }
                    }

                    item(key = "append-footer-$category") {
                        AppendFooter(state = items.loadState.append, onRetry = items::retry)
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = CardBg,
            contentColor = BrandRed,
        )
    }
}

@Composable
private fun FullScreenLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = BrandRed,
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
        Text("\u52a0\u8f7d\u5931\u8d25", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextCaption, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onRetry) {
            Text("\u70b9\u51fb\u91cd\u8bd5", color = BrandRed, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FullScreenEmpty() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("\u6682\u65e0\u5185\u5bb9", color = TextCaption, fontSize = 14.sp)
    }
}

@Composable
private fun AppendFooter(state: LoadState, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is LoadState.Loading -> CircularProgressIndicator(
                color = BrandRed,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )

            is LoadState.Error -> TextButton(onClick = onRetry) {
                Text("\u52a0\u8f7d\u5931\u8d25\uff0c\u70b9\u51fb\u91cd\u8bd5", color = TextSecondary, fontSize = 13.sp)
            }

            is LoadState.NotLoading -> Surface(
                color = CardBg,
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Text(
                    text = if (state.endOfPaginationReached) {
                        "\u5df2\u7ecf\u5230\u5e95\u4e86"
                    } else {
                        "\u7ee7\u7eed\u4e0a\u6ed1\u52a0\u8f7d\u66f4\u591a"
                    },
                    color = TextCaption,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
