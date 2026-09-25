package hu.bme.aut.projection

import hu.bme.aut.events.EventEnvelope
import hu.bme.aut.events.EventJson
import hu.bme.aut.events.UserEvents
import hu.bme.aut.events.UserRegistered
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaTemplate
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootApplication
class ProjectionTestApplication

@TestConfiguration(proxyBeanMethods = false)
class ProjectionContainers {
    @Bean @ServiceConnection fun postgres() = PostgreSQLContainer("postgres:16-alpine")
    @Bean @ServiceConnection fun kafka() = KafkaContainer("apache/kafka:4.3.1")
    @Bean fun userEventsTopic() = TopicBuilder.name(UserEvents.TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).compact().build()
    @Bean fun deadLetterTopic() = TopicBuilder.name(UserEvents.DEAD_LETTER_TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).build()
}

@SpringBootTest(classes = [ProjectionTestApplication::class])
@Import(ProjectionContainers::class)
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

    fun deadLetters(): List<String> = KafkaConsumer<String, String>(
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to kafkaConnection.bootstrapServers.joinToString(","),
            ConsumerConfig.GROUP_ID_CONFIG to "dlt-reader-${UUID.randomUUID()}",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
    ).use { consumer ->
        consumer.subscribe(listOf(UserEvents.DEAD_LETTER_TOPIC))
        val values = mutableListOf<String>()
        repeat(10) { consumer.poll(Duration.ofMillis(500)).forEach { values += it.value() } }
        values
    }
}
