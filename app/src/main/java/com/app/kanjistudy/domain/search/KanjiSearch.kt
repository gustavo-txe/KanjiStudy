package com.app.kanjistudy.domain.search

import com.app.kanjistudy.domain.model.Kanji

object KanjiSearch {
    private val separators = Regex("[\\s,;:.!?()\\[\\]{}\\-_/、。・]+")

    fun filter(kanjis: List<Kanji>, query: String, level: Int?): List<Kanji> {
        val normalizedQuery = query.trim().lowercase()
        return kanjis.filter { kanji ->
            (level == null || kanji.jlpt == level) &&
                (normalizedQuery.isBlank() || kanji.kanji == normalizedQuery ||
                    (kanji.kunReadings + kanji.onReadings + kanji.meanings).any {
                        val normalized = it.trim().lowercase()
                        normalized == normalizedQuery || normalized.split(separators).any { term ->
                            term == normalizedQuery
                        }
                    })
        }
    }
}
