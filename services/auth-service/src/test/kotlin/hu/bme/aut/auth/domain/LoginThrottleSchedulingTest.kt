package hu.bme.aut.auth.domain

import hu.bme.aut.auth.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.config.ScheduledTaskHolder

/** Eviction runs on its own schedule, also when the outbox publisher is switched off (as in these tests). */
class LoginThrottleSchedulingTest : IntegrationTest() {

    @Autowired lateinit var taskHolders: List<ScheduledTaskHolder>

    @Test
    fun expiredThrottleCountersAreEvictedOnASchedule() {
        val tasks = taskHolders.flatMap { it.scheduledTasks }.map { it.task.runnable.toString() }
        assertThat(tasks).anyMatch { it.endsWith("LoginThrottle.evictExpired") }
    }
}
