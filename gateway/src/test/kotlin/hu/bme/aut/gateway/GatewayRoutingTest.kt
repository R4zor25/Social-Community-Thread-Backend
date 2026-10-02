package hu.bme.aut.gateway

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class GatewayRoutingTest : GatewayTestSupport() {

    private fun get(path: String) = client.get().uri(path).header("Authorization", "Bearer ${token()}").exchange()

    @Test
    fun eachPrefixReachesItsBackend() {
        mapOf(
            "/api/v2/users/me" to "auth",
            "/api/v2/threads/1/posts" to "thread",
            "/api/v2/posts?authorId=1" to "thread",
            "/api/v2/comments/7/vote" to "thread",
            "/api/v2/feed" to "thread",
            "/api/v2/friends" to "friend",
            "/api/v2/friend-requests/3" to "friend",
            "/api/v2/conversations/5/messages" to "chat"
        ).forEach { (path, backend) ->
            get(path).expectStatus().isOk.expectBody().jsonPath("$.backend").isEqualTo(backend)
        }
    }

    @Test
    fun theKeySetIsNotRoutedToTheOutside() {
        get("/.well-known/jwks.json").expectStatus().isNotFound
    }

    @Test
    fun unknownPathsAre404() {
        get("/api/v1/threads").expectStatus().isNotFound
    }

    @Test
    fun bodiesOverSixMegabytesAre413() {
        client.put().uri("/api/v2/posts/1/attachment")
            .header("Authorization", "Bearer ${token()}")
            .contentType(MediaType.IMAGE_PNG)
            .bodyValue(ByteArray(7 * 1024 * 1024))
            .exchange()
            .expectStatus().isEqualTo(413)
    }

    @Test
    fun aFiveMegabyteUploadPasses() {
        client.put().uri("/api/v2/posts/1/attachment")
            .header("Authorization", "Bearer ${token()}")
            .contentType(MediaType.IMAGE_PNG)
            .bodyValue(ByteArray(5 * 1024 * 1024))
            .exchange()
            .expectStatus().isOk
    }

    /** auth-service throttles logins per client address, so the gateway must pass on the real one and only that. */
    @Test
    fun theBackendSeesTheClientAddressNotOneTheClientClaims() {
        client.post().uri("/api/v2/auth/login")
            .header("X-Forwarded-For", "203.0.113.66")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"username":"a","password":"b"}""")
            .exchange()
            .expectStatus().isOk

        assertThat(backends.getValue("auth").forwardedFor).containsExactly("127.0.0.1")
    }
}
