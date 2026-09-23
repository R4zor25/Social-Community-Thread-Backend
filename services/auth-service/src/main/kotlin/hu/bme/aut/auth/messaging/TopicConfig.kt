package hu.bme.aut.auth.messaging

import hu.bme.aut.events.UserEvents
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

/** auth-service owns user-events, so it creates the topic and its dead-letter topic. */
@Configuration(proxyBeanMethods = false)
class TopicConfig {

    @Bean
    fun userEventsTopic() = TopicBuilder.name(UserEvents.TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).build()

    // Same partition count: the dead-letter recoverer writes to the partition the record came from.
    @Bean
    fun userEventsDeadLetterTopic() = TopicBuilder.name(UserEvents.DEAD_LETTER_TOPIC).partitions(UserEvents.PARTITIONS).replicas(1).build()
}
