package hu.bme.aut.auth.domain

import org.springframework.scheduling.annotation.Scheduled
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TooManyLoginAttemptsException(val retryAfter: Duration) : RuntimeException("Too many failed login attempts")

/**
 * Counts failed logins per username and per client IP within a sliding window.
 * The counters live in memory, so the limits apply per instance.
 */
class LoginThrottle(
    private val maxFailuresPerUsername: Int,
    private val maxFailuresPerIp: Int,
    private val window: Duration,
    private val clock: Clock
) {
    private val failuresByUsername = ConcurrentHashMap<String, ArrayDeque<Instant>>()
    private val failuresByIp = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    fun check(username: String, ip: String) {
        val now = clock.instant()
        val retryAfter = listOfNotNull(
            retryAfter(failuresByUsername, username.lowercase(), maxFailuresPerUsername, now),
            retryAfter(failuresByIp, ip, maxFailuresPerIp, now)
        ).maxOrNull()
        if (retryAfter != null) throw TooManyLoginAttemptsException(retryAfter)
    }

    fun recordFailure(username: String, ip: String) {
        val now = clock.instant()
        record(failuresByUsername, username.lowercase(), now)
        record(failuresByIp, ip, now)
    }

    fun recordSuccess(username: String) {
        failuresByUsername.remove(username.lowercase())
    }

    /** Drops counters whose failures have all expired; without this, every username ever tried would stay in memory. */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    fun evictExpired(): Int {
        val now = clock.instant()
        var evicted = 0
        listOf(failuresByUsername, failuresByIp).forEach { failures ->
            failures.keys.forEach { key ->
                failures.computeIfPresent(key) { _, times ->
                    synchronized(times) {
                        prune(times, now)
                        if (times.isEmpty()) null.also { evicted++ } else times
                    }
                }
            }
        }
        return evicted
    }

    // compute() keeps a concurrent eviction from dropping the deque this failure is added to.
    private fun record(failures: ConcurrentHashMap<String, ArrayDeque<Instant>>, key: String, now: Instant) {
        failures.compute(key) { _, existing ->
            (existing ?: ArrayDeque()).also { times ->
                synchronized(times) {
                    prune(times, now)
                    times.addLast(now)
                }
            }
        }
    }

    /** Null when below the limit; otherwise how long until enough failures expire to drop below it. */
    private fun retryAfter(failures: ConcurrentHashMap<String, ArrayDeque<Instant>>, key: String, limit: Int, now: Instant): Duration? {
        val times = failures[key] ?: return null
        synchronized(times) {
            prune(times, now)
            if (times.size < limit) return null
            return Duration.between(now, times[times.size - limit].plus(window))
        }
    }

    private fun prune(times: ArrayDeque<Instant>, now: Instant) {
        while (times.isNotEmpty() && !times.first().plus(window).isAfter(now)) times.removeFirst()
    }
}
