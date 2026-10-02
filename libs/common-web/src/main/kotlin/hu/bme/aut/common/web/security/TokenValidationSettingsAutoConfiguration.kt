package hu.bme.aut.common.web.security

import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.security.oauth2.jwt.JwtDecoder

/**
 * When Boot builds the token decoder from properties, it simply leaves out the issuer or audience check if that
 * property is missing. A service then accepts tokens meant for someone else, so it refuses to start instead.
 */
@AutoConfiguration(before = [OAuth2ResourceServerAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(JwtDecoder::class)
@ConditionalOnMissingBean(JwtDecoder::class)
class TokenValidationSettingsAutoConfiguration {

    @Bean
    fun tokenValidationSettingsCheck(environment: Environment) = InitializingBean {
        listOf(ISSUER, AUDIENCES).forEach { check(!environment.getProperty(it).isNullOrBlank()) { "Set $it" } }
    }

    companion object {
        const val ISSUER = "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        const val AUDIENCES = "spring.security.oauth2.resourceserver.jwt.audiences"
    }
}
