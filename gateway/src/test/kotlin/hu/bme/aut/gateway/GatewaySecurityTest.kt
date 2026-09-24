package hu.bme.aut.gateway

import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import java.time.Instant

class GatewaySecurityTest : GatewayTestSupport() {

    @Test
    fun requestsWithoutATokenAre401ProblemsAndNeverReachABackend() {
        client.get().uri("/api/v2/threads").exchange()
            .expectStatus().isUnauthorized
            .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
            .expectBody().jsonPath("$.status").isEqualTo(401)

        assertThat(backends.getValue("thread").requests).isEmpty()
    }

    @Test
    fun publicAuthEndpointsPassWithoutAToken() {
        listOf("register", "login", "refresh", "logout").forEach {
            client.post().uri("/api/v2/auth/$it").contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk
        }
        assertThat(backends.getValue("auth").requests.map { it.first })
            .containsExactly("/api/v2/auth/register", "/api/v2/auth/login", "/api/v2/auth/refresh", "/api/v2/auth/logout")
    }

    @Test
    fun otherAuthPathsNeedAToken() {
        client.get().uri("/api/v2/users/me").exchange().expectStatus().isUnauthorized
        client.get().uri("/api/v2/auth/login").exchange().expectStatus().isUnauthorized
    }

    @Test
    fun aValidTokenIsForwardedWithTheRequest() {
        val token = token()

        client.get().uri("/api/v2/threads").header("Authorization", "Bearer $token").exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.backend").isEqualTo("thread")

        assertThat(backends.getValue("thread").requests.single()).isEqualTo("/api/v2/threads" to "Bearer $token")
    }

    @Test
    fun aTokenSignedByAnotherKeyIs401() {
        val other = RSAKeyGenerator(2048).keyID("test-key").generate()
        client.get().uri("/api/v2/threads").header("Authorization", "Bearer ${token(key = other)}").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun aTokenForAnotherAudienceIs401() {
        client.get().uri("/api/v2/threads").header("Authorization", "Bearer ${token(audience = "another-app")}").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun aTokenFromAnotherIssuerIs401() {
        client.get().uri("/api/v2/threads").header("Authorization", "Bearer ${token(issuer = "http://elsewhere")}").exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun anExpiredTokenIs401() {
        client.get().uri("/api/v2/threads").header("Authorization", "Bearer ${token(expiresAt = Instant.now().minusSeconds(3600))}").exchange()
            .expectStatus().isUnauthorized
            .expectHeader().valueEquals("WWW-Authenticate", "Bearer error=\"invalid_token\"")
    }
}
