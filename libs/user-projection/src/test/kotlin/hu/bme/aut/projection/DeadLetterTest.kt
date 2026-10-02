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

    /** A short database outage is not the event's fault: it is retried until it can be applied, never dead-lettered. */
    @Test
    fun aValidEventSurvivesADatabaseOutage() {
        val event = registered(202, "irene")
        jdbc.execute("alter table user_projection rename to user_projection_offline")
        try {
            send("202", event)
            Thread.sleep(6000)
        } finally {
            jdbc.execute("alter table user_projection_offline rename to user_projection")
        }

        assertThat(awaitValue(Duration.ofSeconds(60)) { username(202) }).isEqualTo("irene")
        assertThat(deadLetters()).noneMatch { it == event }
    }
}
