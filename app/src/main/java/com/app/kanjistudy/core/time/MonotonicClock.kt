package com.app.kanjistudy.core.time

import android.os.SystemClock
import javax.inject.Inject

class MonotonicClock @Inject constructor() {
    fun nowMillis(): Long = SystemClock.elapsedRealtime()
}
