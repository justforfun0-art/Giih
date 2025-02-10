package com.example.gigwork.data.api

import com.example.gigwork.core.error.model.AppError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimiter @Inject constructor() {
    private val timestamps = ConcurrentHashMap<String, RateLimit>()
    private val mutex = Mutex()

    companion object {
        private const val DEFAULT_RATE_LIMIT = 30 // requests per window
        private const val DEFAULT_WINDOW_MS = 60_000L // 1 minute window
        private const val TAG = "RateLimiter"
    }

    data class RateLimit(
        val timestamp: Long,
        val count: Int,
        val maxRequests: Int,
        val windowMs: Long
    )

    /**
     * Checks if a request can be made for the given key
     * @param key The key to check rate limit for
     * @param maxRequests Maximum number of requests allowed in the window
     * @param windowMs Time window in milliseconds
     * @return true if request is allowed, false if rate limit exceeded
     */
    suspend fun checkRateLimit(
        key: String,
        maxRequests: Int = DEFAULT_RATE_LIMIT,
        windowMs: Long = DEFAULT_WINDOW_MS
    ): Boolean = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val limit = timestamps[key]

        when {
            // First request
            limit == null -> {
                timestamps[key] = RateLimit(currentTime, 1, maxRequests, windowMs)
                true
            }
            // Window expired, reset counter
            currentTime - limit.timestamp >= limit.windowMs -> {
                timestamps[key] = RateLimit(currentTime, 1, maxRequests, windowMs)
                true
            }
            // Within window and under limit
            limit.count < limit.maxRequests -> {
                timestamps[key] = limit.copy(count = limit.count + 1)
                true
            }
            // Rate limit exceeded
            else -> false
        }
    }

    /**
     * Get remaining requests for the key
     */
    suspend fun getRemainingRequests(key: String): Int = mutex.withLock {
        val limit = timestamps[key] ?: return DEFAULT_RATE_LIMIT
        val currentTime = System.currentTimeMillis()

        when {
            currentTime - limit.timestamp >= limit.windowMs -> DEFAULT_RATE_LIMIT
            else -> (limit.maxRequests - limit.count).coerceAtLeast(0)
        }
    }

    /**
     * Get time until rate limit reset in milliseconds
     */
    suspend fun getTimeUntilReset(key: String): Long = mutex.withLock {
        val limit = timestamps[key] ?: return 0L
        val currentTime = System.currentTimeMillis()
        val timeElapsed = currentTime - limit.timestamp

        when {
            timeElapsed >= limit.windowMs -> 0L
            else -> limit.windowMs - timeElapsed
        }
    }

    /**
     * Reset rate limit for a specific key
     */
    suspend fun reset(key: String) = mutex.withLock {
        timestamps.remove(key)
    }

    /**
     * Reset all rate limits
     */
    suspend fun resetAll() = mutex.withLock {
        timestamps.clear()
    }

    /**
     * Check if rate limit is exceeded without incrementing counter
     */
    suspend fun isRateLimited(key: String): Boolean = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val limit = timestamps[key] ?: return false

        when {
            currentTime - limit.timestamp >= limit.windowMs -> false
            limit.count >= limit.maxRequests -> true
            else -> false
        }
    }
}