package hu.bme.aut.friend.api

import hu.bme.aut.friend.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
}
