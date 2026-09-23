package hu.bme.aut.auth.api

import com.nimbusds.jose.jwk.JWKSet
import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.auth.domain.RegistrationService
import hu.bme.aut.auth.domain.SessionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.test.web.servlet.get

class JwksTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var sessions: SessionService

    private fun jwks(): JWKSet =
        JWKSet.parse(mockMvc.get("/.well-known/jwks.json").andExpect { status { isOk() } }.andReturn().response.contentAsString)

    @Test
    fun thePublishedKeyVerifiesIssuedTokens() {
        registration.register("alice", "alice@example.com", "correct horse")
        val token = sessions.login("alice", "correct horse", "203.0.113.1").accessToken.value

        val key = jwks().keys.single().toRSAKey()
        val jwt = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build().decode(token)

        assertThat(jwt.headers["kid"]).isEqualTo(key.keyID)
        assertThat(jwt.getClaimAsString("preferred_username")).isEqualTo("alice")
    }

    @Test
    fun theKeySetContainsNoPrivateParts() {
        val key = jwks().keys.single()

        assertThat(key.isPrivate).isFalse()
        assertThat(key.toJSONObject().keys).doesNotContain("d", "p", "q", "dp", "dq", "qi")
    }
}
