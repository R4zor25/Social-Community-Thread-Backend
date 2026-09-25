package hu.bme.aut.projection

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** The documented UserRegistered format (fixture from libs/events) is understood by the consumer. */
class ConsumerContractTest : ProjectionTestSupport() {

    @Test
    fun theFixtureBecomesAProjectionRow() {
        val fixture = javaClass.getResource("/fixtures/user-registered-v1.json")!!.readText()

        send("42", fixture)

        assertThat(awaitValue { username(42) }).isEqualTo("alice")
    }
}
