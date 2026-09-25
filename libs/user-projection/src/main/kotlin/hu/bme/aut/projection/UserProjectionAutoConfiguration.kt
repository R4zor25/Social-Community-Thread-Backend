package hu.bme.aut.projection

import hu.bme.aut.events.UserEvents
import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.util.backoff.FixedBackOff

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate::class, NamedParameterJdbcTemplate::class)
class UserProjectionAutoConfiguration {

    @Bean
    fun userProjections(jdbc: NamedParameterJdbcTemplate) = UserProjections(jdbc)

    @Bean
    fun userEventsConsumer(projections: UserProjections) = UserEventsConsumer(projections)

    /** Three retries a second apart, then the record goes to the dead-letter topic so later events are not blocked. */
    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler::class)
    fun userEventsErrorHandler(template: KafkaTemplate<*, *>): CommonErrorHandler {
        @Suppress("UNCHECKED_CAST")
        val recoverer = DeadLetterPublishingRecoverer(template as KafkaOperations<Any, Any>) { record, _ ->
            TopicPartition(UserEvents.DEAD_LETTER_TOPIC, record.partition())
        }
        return DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3))
    }
}
