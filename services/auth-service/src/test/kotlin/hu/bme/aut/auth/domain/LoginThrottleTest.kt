package hu.bme.aut.auth.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class LoginThrottleTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
        fun advance(duration: Duration) { now = now.plus(duration) }
    }

    private val clock = MutableClock(Instant.parse("2026-01-01T12:00:00Z"))
    private val throttle = LoginThrottle(maxFailuresPerUsername = 5, maxFailuresPerIp = 20, window = Duration.ofMinutes(15), clock = clock)

    private fun fail(username: String, ip: String, times: Int) = repeat(times) { throttle.recordFailure(username, ip) }

    @Test
    fun fiveFailuresForAUsernameBlockIt() {
        fail("alice", "10.0.0.1", 4)
        throttle.check("alice", "10.0.0.1")

        fail("alice", "10.0.0.1", 1)

        assertThatThrownBy { throttle.check("alice", "10.0.0.2") }.isInstanceOf(TooManyLoginAttemptsException::class.java)
    }

    @Test
    fun usernamesAreCaseInsensitive() {
        fail("Alice", "10.0.0.1", 5)

        assertThatThrownBy { throttle.check("alice", "10.0.0.9") }.isInstanceOf(TooManyLoginAttemptsException::class.java)
    }

    @Test
    fun twentyFailuresFromOneIpBlockEveryUsernameFromIt() {
        (1..20).forEach { fail("user$it", "10.0.0.1", 1) }

        assertThatThrownBy { throttle.check("someone-else", "10.0.0.1") }.isInstanceOf(TooManyLoginAttemptsException::class.java)
        throttle.check("someone-else", "10.0.0.2")
    }

    @Test
    fun failuresOutsideTheWindowDoNotCount() {
        fail("alice", "10.0.0.1", 5)
        clock.advance(Duration.ofMinutes(15).plusSeconds(1))

        throttle.check("alice", "10.0.0.1")
    }

    @Test
    fun retryAfterIsTheTimeUntilTheOldestFailureExpires() {
        fail("alice", "10.0.0.1", 1)
        clock.advance(Duration.ofMinutes(5))
        fail("alice", "10.0.0.1", 4)

        assertThatThrownBy { throttle.check("alice", "10.0.0.1") }
            .isInstanceOfSatisfying(TooManyLoginAttemptsException::class.java) {
                assertThat(it.retryAfter).isEqualTo(Duration.ofMinutes(10))
            }
    }

    @Test
    fun successClearsTheUsernameFailures() {
        fail("alice", "10.0.0.1", 4)
        throttle.recordSuccess("alice")
        fail("alice", "10.0.0.1", 4)

        throttle.check("alice", "10.0.0.1")
    }

    @Test
    fun expiredCountersAreEvictedSoRandomUsernamesCannotGrowMemory() {
        fail("random1", "10.0.0.1", 1)
        fail("random2", "10.0.0.1", 1)

        assertThat(throttle.evictExpired()).isZero()
        clock.advance(Duration.ofMinutes(15))

        assertThat(throttle.evictExpired()).isEqualTo(3)
        assertThat(throttle.evictExpired()).isZero()
    }
}
