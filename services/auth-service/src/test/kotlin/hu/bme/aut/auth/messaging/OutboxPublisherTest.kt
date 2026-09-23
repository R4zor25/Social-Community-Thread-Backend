package hu.bme.aut.auth.messaging

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.auth.domain.RegistrationService
import hu.bme.aut.auth.persistence.OutboxRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Clock
import java.util.concurrent.CompletableFuture

class OutboxPublisherTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var publisher: OutboxPublisher

    @Autowired
    lateinit var outbox: OutboxRepository

    @Autowired
    lateinit var kafka: KafkaConnectionDetails

    @Autowired
    lateinit var transactions: org.springframework.transaction.support.TransactionTemplate

    private fun eventIds() = jdbc.queryForList("select id::text from outbox order by created_at", String::class.java)

    @Test
    fun publishesPendingEventsKeyedByUserIdAndMarksThemPublished() {
        val alice = registration.register("alice", "alice@example.com", "correct horse").id!!
        val bob = registration.register("bob", "bob@example.com", "correct horse").id!!
        val ids = eventIds()

        assertThat(publisher.publishPending()).isEqualTo(2)

        val records = readUserEvents(kafka.bootstrapServers.joinToString(","), { r -> ids.any { r.value().contains(it) } }, 2)
        assertThat(records.map { it.key() }).containsExactlyInAnyOrder(alice.toString(), bob.toString())
        assertThat(jdbc.queryForObject("select count(*) from outbox where published_at is null", Long::class.java)).isZero()
        assertThat(publisher.publishPending()).isZero()
    }

    @Test
    fun leavesRowsUnpublishedWhenSendFails() {
        registration.register("alice", "alice@example.com", "correct horse")
        registration.register("bob", "bob@example.com", "correct horse")
        val failing = mockk<KafkaTemplate<String, String>>()
        every { failing.send(any<String>(), any<String>(), any<String>()) } returns
            CompletableFuture.failedFuture<SendResult<String, String>>(IllegalStateException("broker down"))

        val published = transactions.execute { OutboxPublisher(outbox, failing, Clock.systemUTC()).publishPending() }

        assertThat(published).isZero()
        assertThat(jdbc.queryForObject("select count(*) from outbox where published_at is null", Long::class.java)).isEqualTo(2)
        assertThat(publisher.publishPending()).isEqualTo(2)
    }
}
