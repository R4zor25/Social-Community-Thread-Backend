package hu.bme.aut.gateway

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant
import java.util.Date

/** A backend stand-in that records what reached it and answers with its own name. */
class StubBackend(val name: String) {
    val requests = mutableListOf<Pair<String, String?>>()
    val forwardedFor = mutableListOf<String?>()
    private val server = HttpServer.create(InetSocketAddress("localhost", 0), 0).apply {
        createContext("/.well-known/jwks.json") { exchange ->
            val body = JWKSet(signingKey.toPublicJWK()).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        createContext("/") { exchange ->
            exchange.requestBody.readAllBytes()
            synchronized(requests) {
                requests += exchange.requestURI.path to exchange.requestHeaders.getFirst("Authorization")
                forwardedFor += exchange.requestHeaders["X-Forwarded-For"]?.joinToString(",")
            }
            val body = """{"backend":"$name"}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        start()
    }
    val url get() = "http://localhost:${server.address.port}"

    companion object {
        val signingKey: RSAKey = RSAKeyGenerator(2048).keyID("test-key").generate()
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class GatewayTestSupport {

    @Value("\${local.server.port}")
    var port: Int = 0

    lateinit var client: WebTestClient

    @BeforeEach
    fun setUpClient() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").responseTimeout(Duration.ofSeconds(30)).build()
        backends.values.forEach { synchronized(it.requests) { it.requests.clear(); it.forwardedFor.clear() } }
    }

    fun token(
        key: RSAKey = StubBackend.signingKey,
        issuer: String = ISSUER,
        audience: String = AUDIENCE,
        expiresAt: Instant = Instant.now().plusSeconds(900)
    ): String {
        val claims = JWTClaimsSet.Builder()
            .issuer(issuer).audience(audience).subject("42").claim("preferred_username", "alice")
            .issueTime(Date.from(expiresAt.minusSeconds(900))).expirationTime(Date.from(expiresAt))
            .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
            .apply { sign(RSASSASigner(key)) }.serialize()
    }

    companion object {
        const val ISSUER = "http://auth-service"
        const val AUDIENCE = "social-community"
        val backends = listOf("auth", "thread", "friend", "chat").associateWith { StubBackend(it) }

        @JvmStatic
        @DynamicPropertySource
        fun gatewayProperties(registry: DynamicPropertyRegistry) {
            backends.forEach { (name, backend) -> registry.add("gateway.services.$name") { backend.url } }
            registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri") { "${backends.getValue("auth").url}/.well-known/jwks.json" }
            registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri") { ISSUER }
            registry.add("spring.security.oauth2.resourceserver.jwt.audiences") { AUDIENCE }
        }
    }
}
