package hu.bme.aut.friend.api

import hu.bme.aut.friend.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.post
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FriendRequestConcurrencyTest : IntegrationTest() {

    @Test
    fun mutualRequestsAtTheSameTimeLeaveExactlyOneRequest() {
        repeat(5) {
            jdbc.execute("TRUNCATE friend_requests")
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            val results = listOf(alice to 2L, bob to 1L).map { (from, to) ->
                pool.submit<Int> { start.await(); sendRequest(from, to).andReturn().response.status }
            }
            start.countDown()
            val statuses = results.map { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()

            assertThat(statuses).containsExactlyInAnyOrder(201, 409)
            assertThat(count("friend_requests")).isEqualTo(1)
        }
    }

    @Test
    fun parallelAcceptsOfOneRequestGiveOneSuccessAndOne404() {
        repeat(5) {
            jdbc.execute("TRUNCATE friend_requests, friendships")
            sendRequest(alice, 2).andExpect { status { isCreated() } }
            val id = requestId()
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            val results = (1..2).map {
                pool.submit<Int> { start.await(); mockMvc.post("/api/v2/friend-requests/$id/accept") { with(bob) }.andReturn().response.status }
            }
            start.countDown()
            val statuses = results.map { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()

            assertThat(statuses).containsExactlyInAnyOrder(204, 404)
            assertThat(count("friendships")).isEqualTo(1)
        }
    }
}
