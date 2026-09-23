package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.auth.TestKeys
import hu.bme.aut.auth.domain.KeyProvider
import hu.bme.aut.auth.domain.RegistrationService
import hu.bme.aut.auth.domain.SessionService
import hu.bme.aut.auth.domain.TokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class AuthSecurityTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var sessions: SessionService

    private var aliceId = 0L

    @BeforeEach
    fun registerAlice() {
        aliceId = registration.register("alice", "alice@example.com", "correct horse").id!!
    }

    private val sharedKeys get() = KeyProvider(TestKeys.privatePem(TestKeys.shared), TestKeys.publicPem(TestKeys.shared))

    private fun token(
        keys: KeyProvider = sharedKeys,
        issuer: String = "http://auth-service",
        audience: String = "social-community",
        issuedAt: Instant = Instant.now()
    ) = TokenService(keys, issuer, audience, Duration.ofMinutes(15), Clock.fixed(issuedAt, ZoneOffset.UTC)).issue(aliceId, "alice").value

    private fun me(token: String?) = mockMvc.get("/api/v2/users/me") {
        if (token != null) header("Authorization", "Bearer $token")
    }

    @Test
    fun publicEndpointsNeedNoToken() {
        mockMvc.get("/.well-known/jwks.json").andExpect { status { isOk() } }
        mockMvc.post("/api/v2/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isBadRequest() } }
        mockMvc.post("/api/v2/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"x"}"""
        }.andExpect { status { isNoContent() } }
        mockMvc.post("/api/v2/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"x"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.detail") { value("Invalid or expired refresh token") }
        }
    }

    @Test
    fun protectedEndpointWithoutATokenIs401() {
        me(null).andExpect {
            status { isUnauthorized() }
            header { string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")) }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun anyOtherPathAlsoNeedsAToken() {
        mockMvc.get("/api/v2/whatever").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun aTokenFromLoginIsAccepted() {
        me(sessions.login("alice", "correct horse", "203.0.113.1").accessToken.value).andExpect { status { isOk() } }
    }

    @Test
    fun aTokenSignedByAnotherKeyIs401() {
        val otherKeys = TestKeys.generate().let { KeyProvider(TestKeys.privatePem(it), TestKeys.publicPem(it)) }
        me(token(keys = otherKeys)).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun aTokenForAnotherAudienceIs401() {
        me(token(audience = "another-app")).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun aTokenFromAnotherIssuerIs401() {
        me(token(issuer = "http://somewhere-else")).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun anExpiredTokenIs401() {
        me(token(issuedAt = Instant.now().minus(Duration.ofHours(1)))).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun aTamperedTokenIs401() {
        val parts = token().split(".")
        val tampered = parts[0] + "." + parts[1].reversed() + "." + parts[2]
        me(tampered).andExpect { status { isUnauthorized() } }
    }
}
