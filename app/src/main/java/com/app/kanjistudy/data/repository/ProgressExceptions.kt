package com.app.kanjistudy.data.repository

import java.io.IOException

class KanjiCatalogIncompleteException : IllegalStateException("The kanji catalog is incomplete.")

class ProgressRestorationPendingException : IOException("The saved progress has not been fully restored.")
