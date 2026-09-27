package hu.bme.aut.thread

import hu.bme.aut.events.UserEvents
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgres() = PostgreSQLContainer("postgres:16-alpine")

    @Bean
    @ServiceConnection
    fun kafka() = KafkaContainer("apache/kafka:4.3.1")

    // In production auth-service creates these topics; here the consumer needs them to exist.
    @Bean
    fun userEventsTopic() = TopicBuilder.name(UserEvents.TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).compact().build()

    @Bean
    fun userEventsDeadLetterTopic() = TopicBuilder.name(UserEvents.DEAD_LETTER_TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).build()
}
