package com.demo.toutiao.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApi {
    @GET("/api/news")
    suspend fun getNews(
        @Query("category") category: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int = 20,
        @Query("forceRefresh") forceRefresh: Boolean = false,
    ): ApiResponse<NewsListData>
}

class BackendException(val errorCode: Int, message: String) : RuntimeException(message)
