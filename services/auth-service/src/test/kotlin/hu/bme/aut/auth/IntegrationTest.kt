package hu.bme.aut.auth

import org.junit.jupiter.api.BeforeEach
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc

/** Real PostgreSQL and Kafka, one shared Spring context; tables are emptied before each test. */
@SpringBootTest(properties = ["auth.outbox.scheduling-enabled=false"])
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, TestClockConfiguration::class)
abstract class IntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @Autowired
    lateinit var clock: TestClock

    /** Empty tables, and a clock moved past every throttle window left over from earlier tests. */
    @BeforeEach
    fun resetState() {
        jdbc.execute("TRUNCATE users, user_avatars, sessions, refresh_tokens, outbox RESTART IDENTITY CASCADE")
        clock.advance(Duration.ofHours(1))
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
