package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RefreshConcurrencyTest : IntegrationTest() {

    /** Two parallel refreshes with one token must not both win; the second one counts as reuse. */
    @Test
    fun parallelRefreshesWithTheSameTokenGiveExactlyOneSuccess() {
        mockMvc.post("/api/v2/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"alice","email":"alice@example.com","password":"correct horse"}"""
        }.andExpect { status { isCreated() } }
        repeat(5) {
            val refreshToken = loginForRefreshToken()
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(2)
            val statuses = (1..2).map {
                pool.submit<Int> {
                    start.await()
                    mockMvc.post("/api/v2/auth/refresh") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"refreshToken":"$refreshToken"}"""
                    }.andReturn().response.status
                }
            }
            start.countDown()
            val results = statuses.map { it.get(30, TimeUnit.SECONDS) }
            pool.shutdown()

            assertThat(results).containsExactlyInAnyOrder(200, 401)
        }
    }

    private fun loginForRefreshToken(): String {
        val body = mockMvc.post("/api/v2/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"alice","password":"correct horse"}"""
        }.andReturn().response.contentAsString
        return Regex("\"refreshToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }
}
