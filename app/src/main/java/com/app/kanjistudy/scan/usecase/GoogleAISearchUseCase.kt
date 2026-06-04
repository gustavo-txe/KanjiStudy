package com.app.kanjistudy.scan.usecase

import android.net.Uri

fun googleAISearch(kanji: String): String =
    "https://www.google.com/search?q=${Uri.encode("Kanji $kanji")}&udm=50"
