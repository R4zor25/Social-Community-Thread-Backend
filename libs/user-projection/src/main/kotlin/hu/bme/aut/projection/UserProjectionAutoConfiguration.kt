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
import org.springframework.util.backoff.ExponentialBackOff
import tools.jackson.core.JacksonException

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate::class, NamedParameterJdbcTemplate::class)
class UserProjectionAutoConfiguration {

    @Bean
    fun userProjections(jdbc: NamedParameterJdbcTemplate) = UserProjections(jdbc)

    @Bean
    fun userEventsConsumer(projections: UserProjections) = UserEventsConsumer(projections)

    /**
     * A malformed record (not JSON, no type, a payload that does not fit) goes to the dead-letter topic at once, so later
     * events are not blocked by it. Any other failure, such as the database being down, is not the event's fault: it is
     * retried with growing pauses (at most 30 s apart) until it succeeds, keeping the order of the user's events.
     */
    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler::class)
    fun userEventsErrorHandler(template: KafkaTemplate<*, *>): CommonErrorHandler {
        @Suppress("UNCHECKED_CAST")
        val recoverer = DeadLetterPublishingRecoverer(template as KafkaOperations<Any, Any>) { record, _ ->
            TopicPartition(UserEvents.DEAD_LETTER_TOPIC, record.partition())
        }
        val backOff = ExponentialBackOff(1000L, 2.0).apply { maxInterval = 30_000L }
        return DefaultErrorHandler(recoverer, backOff).apply {
            addNotRetryableExceptions(JacksonException::class.java, IllegalArgumentException::class.java)
        }
    }
}
