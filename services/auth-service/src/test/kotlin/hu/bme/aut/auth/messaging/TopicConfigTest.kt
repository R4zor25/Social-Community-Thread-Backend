package hu.bme.aut.auth.messaging

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.events.UserEvents
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.common.config.ConfigResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails
import java.util.concurrent.TimeUnit

/** Services that start later still need every user, so user-events keeps the latest event per user forever. */
class TopicConfigTest : IntegrationTest() {

    @Autowired
    lateinit var kafka: KafkaConnectionDetails

    private fun config(topic: String, key: String): String? =
        AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers.joinToString(","))).use { admin ->
            val resource = ConfigResource(ConfigResource.Type.TOPIC, topic)
            admin.describeConfigs(listOf(resource)).all().get(30, TimeUnit.SECONDS)[resource]?.get(key)?.value()
        }

    @Test
    fun userEventsIsCompacted() {
        assertThat(config(UserEvents.TOPIC, "cleanup.policy")).isEqualTo("compact")
    }

    @Test
    fun theDeadLetterTopicKeepsTheDefaultRetention() {
        assertThat(config(UserEvents.DEAD_LETTER_TOPIC, "cleanup.policy")).isEqualTo("delete")
    }
}
