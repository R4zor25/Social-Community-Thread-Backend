package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RegistrationConcurrencyTest : IntegrationTest() {

    @Test
    fun parallelRegistrationsOfTheSameNameGiveExactlyOneSuccess() {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        val statuses = (1..2).map { i ->
            pool.submit<Int> {
                start.await()
                mockMvc.post("/api/v2/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"username":"alice","email":"alice$i@example.com","password":"correct horse"}"""
                }.andReturn().response.status
            }
        }
        start.countDown()
        val results = statuses.map { it.get(30, TimeUnit.SECONDS) }
        pool.shutdown()

        assertThat(results).containsExactlyInAnyOrder(201, 409)
        assertThat(jdbc.queryForObject("select count(*) from users", Long::class.java)).isEqualTo(1)
    }
}
