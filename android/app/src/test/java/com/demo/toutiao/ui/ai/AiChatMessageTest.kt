package com.demo.toutiao.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiChatMessageTest {
    @Test
    fun `chat messages map to api turns`() {
        val turns = listOf(
            AiChatMessage(AiChatRole.User, "这是什么新闻？"),
            AiChatMessage(AiChatRole.Assistant, "这是一条天气新闻。"),
        ).toApiTurns()

        assertEquals("user", turns[0].role)
        assertEquals("assistant", turns[1].role)
        assertEquals("这是什么新闻？", turns[0].content)
    }
}
