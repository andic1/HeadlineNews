package com.demo.toutiao.data.paging

import com.demo.toutiao.data.db.NewsEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class NewsSourceMergeTest {
    @Test
    fun `refresh stores every fetched item so append can page from database`() {
        val fetchedItems = (1..40).map { index ->
            entity(platform = "toutiao", platformIndex = index).copy(position = 999)
        }

        val storedItems = prepareRefreshItems(fetchedItems)

        assertEquals(40, storedItems.size)
        assertEquals((0 until 40).toList(), storedItems.map { it.position })
    }

    @Test
    fun `keeps successful sources when one source fails`() = runTest {
        val result = fetchMergedNewsEntities(
            platforms = listOf("toutiao", "broken", "tieba"),
            platformLimit = 50,
        ) { platform, platformIndex, _ ->
            when (platform) {
                "broken" -> throw IOException("source down")
                else -> listOf(entity(platform, platformIndex))
            }
        }

        assertEquals(listOf("toutiao-0", "tieba-2"), result.map { it.id })
    }

    @Test(expected = IOException::class)
    fun `throws first source error when every source fails`() = runTest {
        fetchMergedNewsEntities(
            platforms = listOf("toutiao", "tieba"),
            platformLimit = 50,
        ) { _, _, _ ->
            throw IOException("all sources down")
        }
    }

    private fun entity(platform: String, platformIndex: Int): NewsEntity =
        NewsEntity(
            id = "$platform-$platformIndex",
            category = "推荐",
            title = "title-$platform",
            description = null,
            source = platform,
            imageUrl = null,
            originalUrl = "https://example.com/$platform",
            publishTime = null,
            layoutType = "text",
            position = platformIndex,
            cachedAt = 0L,
        )
}
