package hu.bme.aut.apigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.client.RestTemplate


@Configuration
@EnableWebFluxSecurity
open class AppConfig {
    @Bean
    fun template(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    @Throws(Exception::class)
    open fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.authorizeExchange { it ->
            it.anyExchange().permitAll()
            //it.pathMatchers("/api/auth/register", "/api/auth/login", "/api/auth/validate/**, /api/auth/refreshToken")
        }.csrf { it.disable() }.build()
        //http
          //  .csrf{ it.disable() }
            //.authorizeExchange {
            //    authorize(pathMatchers("/resources/**", "/signup", "/about"), permitAll)
            //}
            //.build()
    }


}