package hu.bme.aut.auth.messaging

import hu.bme.aut.auth.TestClockConfiguration
import hu.bme.aut.auth.TestKeys
import hu.bme.aut.auth.TestcontainersConfiguration
import hu.bme.aut.auth.domain.RegistrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.time.Instant

/** With scheduling on (the default outside tests), a registration's event leaves the outbox without any manual call. */
@SpringBootTest(properties = ["auth.outbox.scheduling-enabled=true", "auth.outbox.poll-interval=200ms"])
@Import(TestcontainersConfiguration::class, TestClockConfiguration::class)
class OutboxSchedulerTest {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Test
    fun registrationEventsArePublishedInTheBackground() {
        registration.register("scheduled", "scheduled@example.com", "correct horse")

        val deadline = Instant.now().plus(Duration.ofSeconds(20))
        var unpublished: Long
        do {
            Thread.sleep(200)
            unpublished = jdbc.queryForObject("select count(*) from outbox where published_at is null", Long::class.java)!!
        } while (unpublished > 0 && Instant.now().isBefore(deadline))

        assertThat(unpublished).isZero()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun signingKeys(registry: DynamicPropertyRegistry) {
            registry.add("auth.private-key-path") { TestKeys.sharedPrivateKeyFile.toString() }
            registry.add("auth.public-key-path") { TestKeys.sharedPublicKeyFile.toString() }
        }
    }
}
