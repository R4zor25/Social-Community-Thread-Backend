package hu.bme.aut.auth.domain

import hu.bme.aut.auth.TestKeys
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class TokenServiceTest {

    private val keys = TestKeys.generate()
    private val keyProvider = KeyProvider(TestKeys.privatePem(keys), TestKeys.publicPem(keys))
    private val now = Instant.parse("2026-01-01T12:00:00Z")
    private val tokenService = TokenService(
        keyProvider = keyProvider,
        issuer = "http://auth-service",
        audience = "social-community",
        accessTokenTtl = Duration.ofMinutes(15),
        clock = Clock.fixed(now, ZoneOffset.UTC)
    )

    private fun decode(token: String) = NimbusJwtDecoder.withPublicKey(keys.public as RSAPublicKey).build()
        .also { it.setJwtValidator { org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success() } }
        .decode(token)

    @Test
    fun accessTokenCarriesTheDocumentedClaims() {
        val jwt = decode(tokenService.issue(userId = 42, username = "alice").value)

        assertThat(jwt.subject).isEqualTo("42")
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("alice")
        assertThat(jwt.issuer.toString()).isEqualTo("http://auth-service")
        assertThat(jwt.audience).containsExactly("social-community")
        assertThat(jwt.id).isNotBlank()
    }

    @Test
    fun accessTokenLivesFifteenMinutes() {
        val issued = tokenService.issue(userId = 42, username = "alice")
        val jwt = decode(issued.value)

        assertThat(jwt.issuedAt).isEqualTo(now)
        assertThat(jwt.expiresAt).isEqualTo(now.plus(Duration.ofMinutes(15)))
        assertThat(issued.expiresIn).isEqualTo(Duration.ofMinutes(15))
    }

    @Test
    fun keyIdIsTheKeyThumbprint() {
        val jwt = decode(tokenService.issue(userId = 42, username = "alice").value)

        assertThat(jwt.headers["kid"]).isEqualTo(keyProvider.rsaKey.computeThumbprint().toString())
        assertThat(jwt.headers["alg"].toString()).isEqualTo("RS256")
    }

    @Test
    fun everyTokenHasItsOwnId() {
        val first = decode(tokenService.issue(42, "alice").value)
        val second = decode(tokenService.issue(42, "alice").value)

        assertThat(first.id).isNotEqualTo(second.id)
    }
}
