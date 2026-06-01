package com.headline.news.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NewsApi {
    @GET("/daily-hot/{platform}")
    suspend fun getHotNews(
        @Path("platform") platform: String,
        @Query("cache") cache: Boolean = false,
        @Query("limit") limit: Int = 50,
    ): HotNewsResponse
}

class ApiException(val errorCode: Int, message: String) : RuntimeException(message)
