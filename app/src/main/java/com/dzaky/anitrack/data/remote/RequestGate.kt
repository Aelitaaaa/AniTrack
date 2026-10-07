package com.dzaky.anitrack.data.remote

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RequestGate @Inject constructor() {
    private val mutex = Mutex()
    private var lastStartNanos = 0L

    suspend fun awaitTurn(intervalMillis: Long = 1_100) = mutex.withLock {
        val interval = TimeUnit.MILLISECONDS.toNanos(intervalMillis)
        val remaining = interval - (System.nanoTime() - lastStartNanos)
        if (remaining > 0) delay(TimeUnit.NANOSECONDS.toMillis(remaining) + 1)
        lastStartNanos = System.nanoTime()
    }
}
