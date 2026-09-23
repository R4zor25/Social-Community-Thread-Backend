package hu.bme.aut.auth.messaging

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty("auth.outbox.scheduling-enabled", havingValue = "true", matchIfMissing = true)
class OutboxScheduler(private val publisher: OutboxPublisher) {

    @Scheduled(fixedDelayString = "\${auth.outbox.poll-interval}")
    fun publish() {
        publisher.publishPending()
    }
}
