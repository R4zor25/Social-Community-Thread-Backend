package hu.bme.aut.projection

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class DeadLetterTest : ProjectionTestSupport() {

    @Test
    fun aMalformedRecordGoesToTheDeadLetterTopicAndLaterRecordsStillArrive() {
        val marker = "not-json-${System.nanoTime()}"
        send("201", marker)
        send("201", registered(201, "henry"))

        assertThat(awaitValue(Duration.ofSeconds(60)) { username(201) }).isEqualTo("henry")
        assertThat(deadLetters()).contains(marker)
    }
}
