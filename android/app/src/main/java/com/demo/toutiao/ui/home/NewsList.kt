package com.demo.toutiao.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.demo.toutiao.data.model.NewsItem
import com.demo.toutiao.ui.theme.Bg
import com.demo.toutiao.ui.theme.TextCaption
import com.demo.toutiao.ui.theme.TextSecondary
import com.demo.toutiao.ui.theme.ToutiaoRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsList(
    items: LazyPagingItems<NewsItem>,
    modifier: Modifier = Modifier,
    onNewsClick: (NewsItem) -> Unit = {},
) {
    val ptrState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

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
                    message = refreshState.error.message ?: "网络异常，请稍后重试",
                    onRetry = { items.retry() },
                )
            }
            items.itemCount == 0 -> {
                FullScreenEmpty()
            }
            else -> {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(200)),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().background(Bg),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(
                            count = items.itemCount,
                            key = { idx -> items.peek(idx)?.let { "${it.category}-${it.id}" } ?: idx },
                        ) { idx ->
                            items[idx]?.let { item ->
                                // 列表项入场动画
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow,
                                        ),
                                    ) + fadeIn(tween(200)),
                                ) {
                                    NewsCard(item, onNewsClick = onNewsClick)
                                }
                            }
                        }
                        item {
                            AppendFooter(items.loadState.append) { items.retry() }
                        }
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = ptrState,
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
        Modifier.fillMaxSize(),
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
                Text("加载失败，点击重试", color = TextSecondary, fontSize = 13.sp)
            }
            is LoadState.NotLoading -> if (state.endOfPaginationReached) {
                Text("— 没有更多了 —", color = TextCaption, fontSize = 12.sp)
            }
        }
    }
}
