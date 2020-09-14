package com.flxrs.dankchat.utils.extensions

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

fun ConflatedBroadcastChannel<MutableMap<String, Int>>.increment(key: String, amount: Int) = offer(value.apply {
    val count = get(key) ?: 0
    put(key, count + amount)
})

fun CoroutineScope.timer(interval: Long, action: suspend TimerScope.() -> Unit): Job {
    return launch {
        val scope = TimerScope()

        while (true) {
            try {
                action(scope)
            } catch (ex: Exception) {
                Log.e("TimerScope", Log.getStackTraceString(ex))
            }

            if (scope.isCanceled) {
                break
            }

            delay(interval)
            yield()
        }
    }
}

class TimerScope {
    var isCanceled: Boolean = false
        private set

    fun cancel() {
        isCanceled = true
    }
}