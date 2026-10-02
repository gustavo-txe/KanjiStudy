package com.app.kanjistudy.domain.search

import com.app.kanjistudy.testing.kanji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KanjiSearchTest {
    private val catalog = listOf(
        kanji().copy(meanings = listOf("sun; day")),
        kanji("月", 4).copy(meanings = listOf("moon, month")),
        kanji("明", null).copy(meanings = listOf("sunlight"))
    )

    @Test fun `whole terms are case insensitive and do not match partial words`() {
        assertEquals(listOf(catalog.first()), KanjiSearch.filter(catalog, " SUN ", null))
        assertEquals(listOf(catalog[1]), KanjiSearch.filter(catalog, "month", null))
        assertTrue(KanjiSearch.filter(catalog, "mon", null).isEmpty())
    }

    @Test fun `characters readings and JLPT are combined consistently`() {
        assertEquals(listOf(catalog.first()), KanjiSearch.filter(catalog, "日", 5))
        assertTrue(KanjiSearch.filter(catalog, "日", 4).isEmpty())
        assertEquals(catalog, KanjiSearch.filter(catalog, "", null))
        assertEquals(listOf(catalog[1]), KanjiSearch.filter(catalog, "", 4))
        assertEquals(catalog, KanjiSearch.filter(catalog, "ひ", null))
    }
}
