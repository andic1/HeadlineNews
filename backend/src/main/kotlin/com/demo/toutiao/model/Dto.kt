package com.demo.toutiao.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null,
)

@Serializable
data class NewsListResponse(
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

object LayoutType {
    const val TEXT_ONLY = "TEXT_ONLY"
    const val TEXT_WITH_THUMB = "TEXT_WITH_THUMB"
    const val BIG_IMAGE = "BIG_IMAGE"
}

object ErrorCode {
    const val OK = 0
    const val WHYTA_FAIL = 1001
    const val BAD_PARAM = 1002
    const val INTERNAL = 1003
}
