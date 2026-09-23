package hu.bme.aut.auth

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A clock the tests can move forward, e.g. past a refresh token's lifetime or a throttle window. */
class TestClock(private var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = now
    fun advance(duration: Duration) { now = now.plus(duration) }
}

@TestConfiguration(proxyBeanMethods = false)
class TestClockConfiguration {
    @Bean
    @Primary
    fun testClock() = TestClock(Instant.now())
}
