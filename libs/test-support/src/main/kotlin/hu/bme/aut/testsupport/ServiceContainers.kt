package hu.bme.aut.testsupport

import hu.bme.aut.events.UserEvents
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.TopicBuilder
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * PostgreSQL and Kafka for the integration tests of services that consume user-events.
 * Spring caches the test context, so the containers are shared by every test class with the same configuration.
 */
@TestConfiguration(proxyBeanMethods = false)
class ServiceContainers {

    @Bean
    @ServiceConnection
    fun postgres() = PostgreSQLContainer("postgres:16-alpine")

    @Bean
    @ServiceConnection
    fun kafka() = KafkaContainer("apache/kafka:4.3.1")

    // In production auth-service creates these topics; here the consumers need them to exist.
    @Bean
    fun userEventsTopic() = TopicBuilder.name(UserEvents.TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).compact().build()

    @Bean
    fun userEventsDeadLetterTopic() = TopicBuilder.name(UserEvents.DEAD_LETTER_TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).build()
}
