package hu.bme.aut.apigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain


@Configuration
@EnableWebFluxSecurity
open class AppConfig {

    // Authentication is done by AuthenticationFilter on the gateway routes, not by Spring Security.
    @Bean
    @Throws(Exception::class)
    open fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange { it.anyExchange().permitAll() }
            .csrf { it.disable() }
            .build()
    }
}
