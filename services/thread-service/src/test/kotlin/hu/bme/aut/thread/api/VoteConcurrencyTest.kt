package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VoteConcurrencyTest : IntegrationTest() {

    private fun inParallel(voters: List<RequestPostProcessor>, postId: Long): List<Int> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(16)
        val results = voters.map { voter ->
            pool.submit<Int> {
                start.await()
                mockMvc.put("/api/v2/posts/$postId/vote") {
                    with(voter)
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"direction":"UP"}"""
                }.andReturn().response.status
            }
        }
        start.countDown()
        return results.map { it.get(60, TimeUnit.SECONDS) }.also { pool.shutdown() }
    }

    @Test
    fun parallelVotesFromDifferentUsersAreAllCounted() {
        val post = createPost(createThread())

        val statuses = inParallel((100L until 150L).map { asUser(it, "user$it") }, post)

        assertThat(statuses).containsOnly(204)
        assertThat(jdbc.queryForObject("select score from posts where id = ?", Int::class.java, post)).isEqualTo(50)
        assertThat(count("post_votes")).isEqualTo(50)
    }

    @Test
    fun theSameVoteRepeatedInParallelCountsOnce() {
        val post = createPost(createThread())

        val statuses = inParallel(List(20) { bob }, post)

        assertThat(statuses).containsOnly(204)
        assertThat(jdbc.queryForObject("select score from posts where id = ?", Int::class.java, post)).isEqualTo(1)
    }
}
