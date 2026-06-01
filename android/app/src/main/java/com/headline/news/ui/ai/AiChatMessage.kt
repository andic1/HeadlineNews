package com.headline.news.ui.ai

import com.headline.news.data.api.AiSource
import com.headline.news.data.api.AiChatTurn

enum class AiChatRole {
    User,
    Assistant,
}

data class AiChatMessage(
    val role: AiChatRole,
    val text: String,
    val sources: List<AiSource> = emptyList(),
)

fun List<AiChatMessage>.toApiTurns(): List<AiChatTurn> =
    map { message ->
        AiChatTurn(
            role = when (message.role) {
                AiChatRole.User -> "user"
                AiChatRole.Assistant -> "assistant"
            },
            content = message.text,
        )
    }
