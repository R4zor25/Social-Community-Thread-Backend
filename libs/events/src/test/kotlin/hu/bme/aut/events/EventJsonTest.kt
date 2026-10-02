package hu.bme.aut.events

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.time.Instant
import java.util.UUID

class EventJsonTest {

    private val fixture = javaClass.getResource("/fixtures/user-registered-v1.json")!!.readText()

    private val event = EventEnvelope(
        eventId = UUID.fromString("7f3c2a9e-1b4d-4c8e-9a2f-5d6e7f8a9b0c"),
        type = UserRegistered.TYPE,
        version = UserRegistered.VERSION,
        occurredAt = Instant.parse("2026-01-01T12:00:00Z"),
        payload = UserRegistered(userId = 42, username = "alice")
    )

    @Test
    fun writesTheDocumentedFormat() {
        JSONAssert.assertEquals(fixture, EventJson.write(event), JSONCompareMode.STRICT)
    }

    @Test
    fun readsTheDocumentedFormat() {
        assertThat(EventJson.read(fixture, UserRegistered::class.java)).isEqualTo(event)
    }

    @Test
    fun readsTheTypeWithoutKnowingThePayload() {
        assertThat(EventJson.typeOf(fixture)).isEqualTo("UserRegistered")
    }

    @Test
    fun ignoresUnknownFieldsSoProducersCanAddFields() {
        val extended = fixture.replace("\"username\": \"alice\"", "\"username\": \"alice\", \"displayName\": \"Alice\"")
        assertThat(EventJson.read(extended, UserRegistered::class.java).payload.username).isEqualTo("alice")
    }

    @Test
    fun aRecordWithoutATypeIsMalformed() {
        assertThatThrownBy { EventJson.typeOf("""{"payload":{}}""") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
