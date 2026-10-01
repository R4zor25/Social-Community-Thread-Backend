package hu.bme.aut.chat.api

import hu.bme.aut.chat.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ParticipantConcurrencyTest : IntegrationTest() {

    /** Both leave at once: one of them is the last, so the conversation must not survive empty. */
    @Test
    fun whenTheLastTwoLeaveAtOnceTheConversationIsDeleted() {
        repeat(5) {
            createConversation(participantIds = listOf(2)).andExpect { status { isCreated() } }
            val id = conversationId()
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            val results = listOf(alice to 1L, bob to 2L).map { (who, userId) ->
                pool.submit<Int> {
                    start.await()
                    mockMvc.delete("/api/v2/conversations/$id/participants/$userId") { with(who) }.andReturn().response.status
                }
            }
            start.countDown()
            val statuses = results.map { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()

            assertThat(statuses).allMatch { it == 204 }
            assertThat(count("conversations")).isZero()
        }
    }
}
