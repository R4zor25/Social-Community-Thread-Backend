package hu.bme.aut.gateway

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.BearerTokenError
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Rejects requests without a valid access token before they reach a service. The services validate the token again;
 * the gateway is the first line, not the only one.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

    /**
     * Validates signature, expiry, issuer and audience against the key set of auth-service, fetched lazily.
     * A failure to fetch the keys becomes a JwtException so that it reaches the failure handler instead of a 500.
     */
    @Bean
    fun jwtDecoder(
        @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") issuer: String,
        @Value("\${spring.security.oauth2.resourceserver.jwt.audiences}") audience: String
    ): ReactiveJwtDecoder {
        val delegate = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build().apply {
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(issuer),
                    JwtClaimValidator<List<String>?>(JwtClaimNames.AUD) { it?.contains(audience) == true }
                )
            )
        }
        return ReactiveJwtDecoder { token ->
            delegate.decode(token).onErrorMap({ it !is JwtException }) { JwtException("Could not verify the token", it) }
        }
    }

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain = http
        .csrf { it.disable() }
        .httpBasic { it.disable() }
        .formLogin { it.disable() }
        .authorizeExchange {
            it.pathMatchers(
                HttpMethod.POST,
                "/api/v2/auth/register", "/api/v2/auth/login", "/api/v2/auth/refresh", "/api/v2/auth/logout"
            ).permitAll()
                .pathMatchers("/actuator/health/**").permitAll()
                .anyExchange().authenticated()
        }
        .oauth2ResourceServer {
            it.jwt { }
            it.authenticationFailureHandler(tokenFailureHandler())
            it.authenticationEntryPoint(problemEntryPoint())
            it.accessDeniedHandler(problemAccessDeniedHandler())
        }
        .exceptionHandling {
            it.authenticationEntryPoint(problemEntryPoint())
            it.accessDeniedHandler(problemAccessDeniedHandler())
        }
        .build()

    private fun problemEntryPoint() = ServerAuthenticationEntryPoint { exchange, e ->
        val error = ((e as? OAuth2AuthenticationException)?.error as? BearerTokenError)?.errorCode
        exchange.response.headers.set(HttpHeaders.WWW_AUTHENTICATE, if (error == null) "Bearer" else "Bearer error=\"$error\"")
        writeProblem(exchange, HttpStatus.UNAUTHORIZED, "A valid access token is required")
    }

    /** If the keys cannot be fetched (auth-service down), the token is not known to be bad: 503, not 401. */
    private fun tokenFailureHandler() = ServerAuthenticationFailureHandler { webFilterExchange, e ->
        if (e is AuthenticationServiceException) {
            writeProblem(webFilterExchange.exchange, HttpStatus.SERVICE_UNAVAILABLE, "Access tokens cannot be verified right now")
        } else {
            problemEntryPoint().commence(webFilterExchange.exchange, e)
        }
    }

    private fun problemAccessDeniedHandler() = ServerAccessDeniedHandler { exchange, _ ->
        exchange.response.headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"insufficient_scope\"")
        writeProblem(exchange, HttpStatus.FORBIDDEN, "Access denied")
    }

    private fun writeProblem(exchange: ServerWebExchange, status: HttpStatus, detail: String): Mono<Void> {
        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_PROBLEM_JSON
        val body = """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},"detail":"$detail","instance":"${exchange.request.path.value()}"}"""
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.toByteArray())))
    }
}
