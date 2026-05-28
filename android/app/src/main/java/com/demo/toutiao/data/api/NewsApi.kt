package com.demo.toutiao.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface NewsApi {
    @GET("/daily-hot/{platform}")
    suspend fun getHotNews(
        @Path("platform") platform: String,
    ): HotNewsResponse
}

class ApiException(val errorCode: Int, message: String) : RuntimeException(message)
