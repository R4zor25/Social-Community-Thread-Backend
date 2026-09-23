package hu.bme.aut.auth.messaging

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.auth.domain.RegistrationService
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails

/** The record auth-service sends has the shape documented by the libs/events fixture. */
class ProducerContractTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var publisher: OutboxPublisher

    @Autowired
    lateinit var kafka: KafkaConnectionDetails

    @Test
    fun publishedUserRegisteredMatchesTheFixture() {
        registration.register("alice", "alice@example.com", "correct horse")
        val eventId = jdbc.queryForObject("select id::text from outbox", String::class.java)!!
        publisher.publishPending()

        val record = readUserEvents(kafka.bootstrapServers.joinToString(","), { it.value().contains(eventId) }, 1).single()

        val fixture = javaClass.getResource("/fixtures/user-registered-v1.json")!!.readText()
        val anyValue = { _: Any?, _: Any? -> true }
        JSONAssert.assertEquals(
            fixture, record.value(),
            CustomComparator(
                JSONCompareMode.STRICT,
                Customization("eventId", anyValue),
                Customization("occurredAt", anyValue),
                Customization("payload.userId", anyValue)
            )
        )
    }
}
