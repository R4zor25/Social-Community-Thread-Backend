package hu.bme.aut.common.web.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner

/** Without issuer or audience, Boot would build a decoder that silently skips those checks. */
class TokenValidationSettingsTest {

    private val runner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TokenValidationSettingsAutoConfiguration::class.java))
        .withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://auth-service/.well-known/jwks.json")

    @Test
    fun startsWithIssuerAndAudience() {
        runner.withPropertyValues(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-service",
            "spring.security.oauth2.resourceserver.jwt.audiences=social-community"
        ).run { assertThat(it).hasNotFailed() }
    }

    @Test
    fun failsWithoutIssuer() {
        runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.audiences=social-community").run {
            assertThat(it).hasFailed()
            assertThat(it.startupFailure).hasRootCauseMessage("Set spring.security.oauth2.resourceserver.jwt.issuer-uri")
        }
    }

    @Test
    fun failsWithoutAudience() {
        runner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-service").run {
            assertThat(it).hasFailed()
            assertThat(it.startupFailure).hasRootCauseMessage("Set spring.security.oauth2.resourceserver.jwt.audiences")
        }
    }
}
