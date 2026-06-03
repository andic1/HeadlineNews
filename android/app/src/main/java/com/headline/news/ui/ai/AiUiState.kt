package com.headline.news.ui.ai

sealed interface AiUiState<out T> {
    data object Idle : AiUiState<Nothing>
    data object Loading : AiUiState<Nothing>
    data class Success<T>(val data: T) : AiUiState<T>
    data class Error(val message: String) : AiUiState<Nothing>
}

fun Throwable.toUserMessage(): String {
    val raw = message.orEmpty()
    return when {
        raw.contains("401") -> "智能服务校验失败，请检查本机配置"
        raw.contains("503") || raw.contains("AI_API_KEY") -> "智能服务还没有配置好模型 Key"
        raw.contains("timeout", ignoreCase = true) -> "智能服务响应超时，请稍后再试"
        raw.isNotBlank() -> "智能服务暂时不可用：$raw"
        else -> "智能服务暂时不可用，请稍后再试"
    }
}
