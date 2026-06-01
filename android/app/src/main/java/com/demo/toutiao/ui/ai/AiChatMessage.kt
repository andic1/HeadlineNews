package com.demo.toutiao.ui.ai

import com.demo.toutiao.data.api.AiSource
import com.demo.toutiao.data.api.AiChatTurn

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
