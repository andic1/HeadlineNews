package com.demo.toutiao.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null,
)

@Serializable
data class NewsListData(
    val category: String,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
    val fromCache: Boolean,
    val list: List<NewsDto>,
)

@Serializable
data class NewsDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val source: String? = null,
    val publishTime: String? = null,
    val imageUrl: String? = null,
    val originalUrl: String? = null,
    val layoutType: String,
)
