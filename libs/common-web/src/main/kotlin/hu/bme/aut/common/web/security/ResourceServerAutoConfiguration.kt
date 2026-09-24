package hu.bme.aut.common.web.security

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain

/** Default for every service: stateless, JWT required except for health and API docs. Replaced by a service's own chain. */
@AutoConfiguration(before = [OAuth2ResourceServerAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ResourceServerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    fun resourceServerSecurityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/error").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer {
            it.jwt { }
            it.authenticationEntryPoint(ProblemAuthenticationEntryPoint())
            it.accessDeniedHandler(ProblemAccessDeniedHandler())
            it.withObjectPostProcessor(object : ObjectPostProcessor<BearerTokenAuthenticationFilter> {
                override fun <O : BearerTokenAuthenticationFilter> postProcess(filter: O): O = filter.apply {
                    setAuthenticationFailureHandler(ProblemAuthenticationFailureHandler(ProblemAuthenticationEntryPoint()))
                }
            })
        }
        .exceptionHandling {
            it.authenticationEntryPoint(ProblemAuthenticationEntryPoint())
            it.accessDeniedHandler(ProblemAccessDeniedHandler())
        }
        .build()
}
