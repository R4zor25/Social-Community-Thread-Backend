package hu.bme.aut.projection

import hu.bme.aut.events.EventJson
import hu.bme.aut.events.UserEvents
import hu.bme.aut.events.UserRegistered
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener

/** Upserts are idempotent, so redelivered events are harmless. Event types this service does not know are skipped. */
class UserEventsConsumer(private val projections: UserProjections) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [UserEvents.TOPIC])
    fun onEvent(json: String) {
        when (val type = EventJson.typeOf(json)) {
            UserRegistered.TYPE -> EventJson.read(json, UserRegistered::class.java).payload.let { projections.upsert(it.userId, it.username) }
            else -> log.debug("Skipping user event of type {}", type)
        }
    }
}
