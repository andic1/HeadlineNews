package com.demo.toutiao.ui.ai

import com.demo.toutiao.data.api.AiSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AiChatMessageTest {
    @Test
    fun `chat messages map to api turns`() {
        val turns = listOf(
            AiChatMessage(AiChatRole.User, "what is this news"),
            AiChatMessage(AiChatRole.Assistant, "it is weather news"),
        ).toApiTurns()

        assertEquals("user", turns[0].role)
        assertEquals("assistant", turns[1].role)
        assertEquals("what is this news", turns[0].content)
    }

    @Test
    fun `assistant message can carry sources`() {
        val message = AiChatMessage(
            role = AiChatRole.Assistant,
            text = "summary",
            sources = listOf(AiSource(title = "source", url = "https://example.com", snippet = "snippet")),
        )

        assertEquals("source", message.sources[0].title)
        assertEquals("https://example.com", message.sources[0].url)
    }
}
