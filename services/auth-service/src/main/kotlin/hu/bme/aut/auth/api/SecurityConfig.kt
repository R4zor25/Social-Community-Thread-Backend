package hu.bme.aut.auth.api

import hu.bme.aut.auth.AuthProperties
import hu.bme.aut.auth.domain.KeyProvider
import hu.bme.aut.common.web.security.ProblemAccessDeniedHandler
import hu.bme.aut.common.web.security.ProblemAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /** auth-service validates its own tokens with its public key instead of fetching its own JWKS. */
    @Bean
    fun jwtDecoder(keyProvider: KeyProvider, properties: AuthProperties): JwtDecoder =
        NimbusJwtDecoder.withPublicKey(keyProvider.publicKey).build().apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(properties.issuer),
                    JwtClaimValidator<List<String>?>(JwtClaimNames.AUD) { it?.contains(properties.audience) == true }
                )
            )
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(
                HttpMethod.POST,
                "/api/v2/auth/register", "/api/v2/auth/login", "/api/v2/auth/refresh", "/api/v2/auth/logout"
            ).permitAll()
                .requestMatchers(HttpMethod.GET, "/.well-known/jwks.json").permitAll()
                .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/error").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer {
            it.jwt { }
            it.authenticationEntryPoint(ProblemAuthenticationEntryPoint())
            it.accessDeniedHandler(ProblemAccessDeniedHandler())
        }
        .exceptionHandling {
            it.authenticationEntryPoint(ProblemAuthenticationEntryPoint())
            it.accessDeniedHandler(ProblemAccessDeniedHandler())
        }
        .build()
}
