package hu.bme.aut.auth.messaging

import hu.bme.aut.auth.persistence.OutboxRepository
import hu.bme.aut.events.UserEvents
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * Sends outbox rows in creation order, keyed by user id, and marks them published.
 * Stops at the first failure so that later events never overtake an unsent one; delivery is at least once.
 */
@Component
class OutboxPublisher(
    private val outbox: OutboxRepository,
    private val kafka: KafkaTemplate<String, String>,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun publishPending(batchSize: Int = 100): Int {
        var published = 0
        for (event in outbox.lockUnpublished(batchSize)) {
            try {
                kafka.send(UserEvents.TOPIC, event.aggregateId, event.payload).get(10, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                log.warn("Could not publish outbox event {}; will retry", event.id, e)
                break
            }
            event.publishedAt = clock.instant()
            published++
        }
        return published
    }
}
