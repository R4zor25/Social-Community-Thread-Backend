package hu.bme.aut.projection

import hu.bme.aut.events.EventEnvelope
import hu.bme.aut.events.EventJson
import hu.bme.aut.events.UserEvents
import hu.bme.aut.events.UserRegistered
import hu.bme.aut.testsupport.ServiceContainers
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootApplication
class ProjectionTestApplication

@SpringBootTest(classes = [ProjectionTestApplication::class])
@Import(ServiceContainers::class)
abstract class ProjectionTestSupport {

    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var kafka: KafkaTemplate<String, String>
    @Autowired lateinit var kafkaConnection: KafkaConnectionDetails
    @Autowired lateinit var projections: UserProjections

    @BeforeEach
    fun cleanTable() {
        jdbc.execute("TRUNCATE user_projection")
    }

    fun registered(userId: Long, username: String) = EventJson.write(
        EventEnvelope(UUID.randomUUID(), UserRegistered.TYPE, UserRegistered.VERSION, Instant.now(), UserRegistered(userId, username))
    )

    fun send(key: String, value: String) {
        kafka.send(UserEvents.TOPIC, key, value).get()
    }

    fun username(userId: Long): String? =
        jdbc.queryForList("select username from user_projection where user_id = ?", String::class.java, userId).singleOrNull()

    fun <T> awaitValue(timeout: Duration = Duration.ofSeconds(30), read: () -> T?): T? {
        val deadline = Instant.now().plus(timeout)
        while (Instant.now().isBefore(deadline)) {
            read()?.let { return it }
            Thread.sleep(100)
        }
        return null
    }

    /** Everything on the dead-letter topic right now: reads every partition from the start up to its end offset. */
    fun deadLetters(): List<String> = KafkaConsumer<String, String>(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaConnection.bootstrapServers.joinToString(","),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
    ).use { consumer ->
        val partitions = consumer.partitionsFor(UserEvents.DEAD_LETTER_TOPIC).map { TopicPartition(it.topic(), it.partition()) }
        consumer.assign(partitions)
        consumer.seekToBeginning(partitions)
        val end = consumer.endOffsets(partitions)
        val values = mutableListOf<String>()
        while (partitions.any { consumer.position(it) < end.getValue(it) }) {
            consumer.poll(Duration.ofMillis(500)).forEach { values += it.value() }
        }
        values
    }
}
