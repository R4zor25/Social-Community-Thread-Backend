package hu.bme.aut.auth.messaging

import hu.bme.aut.events.UserEvents
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Reads user-events from the beginning until the wanted records arrive; other tests' records are skipped. */
fun readUserEvents(bootstrapServers: String, wanted: (ConsumerRecord<String, String>) -> Boolean, count: Int): List<ConsumerRecord<String, String>> {
    val consumer = KafkaConsumer<String, String>(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "test-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
    )
    consumer.use {
        it.subscribe(listOf(UserEvents.TOPIC))
        val found = mutableListOf<ConsumerRecord<String, String>>()
        val deadline = Instant.now().plusSeconds(30)
        while (found.size < count && Instant.now().isBefore(deadline)) {
            it.poll(Duration.ofMillis(500)).filter(wanted).forEach(found::add)
        }
        return found
    }
}
