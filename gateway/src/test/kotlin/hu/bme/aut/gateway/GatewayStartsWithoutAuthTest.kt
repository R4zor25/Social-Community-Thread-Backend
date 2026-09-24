package hu.bme.aut.gateway

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient

/** The key set is fetched lazily: the gateway starts while auth-service is down and answers 503 until it can verify tokens. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayStartsWithoutAuthTest {

    @Value("\${local.server.port}")
    var port: Int = 0

    private val thread = StubBackend("thread")

    @Test
    fun startsAndAnswers503WhileTheKeysCannotBeFetched() {
        val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()
        val token = object : GatewayTestSupport() {}.token()

        client.get().uri("/api/v2/threads").header("Authorization", "Bearer $token").exchange()
            .expectStatus().isEqualTo(503)
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)

        assertThat(thread.requests).isEmpty()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun unreachableAuth(registry: DynamicPropertyRegistry) {
            listOf("auth", "thread", "friend", "chat").forEach { registry.add("gateway.services.$it") { "http://localhost:1" } }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "http://localhost:1/.well-known/jwks.json" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { GatewayTestSupport.ISSUER }
            registry.add("spring.security.oauth2.resourceserver.jwt.audiences") { GatewayTestSupport.AUDIENCE }
        }
    }
}
