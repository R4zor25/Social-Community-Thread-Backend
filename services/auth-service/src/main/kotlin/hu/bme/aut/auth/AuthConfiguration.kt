package hu.bme.aut.auth

import hu.bme.aut.auth.domain.KeyProvider
import hu.bme.aut.auth.domain.LoginThrottle
import hu.bme.aut.auth.domain.TokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class AuthConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun keyProvider(properties: AuthProperties) = KeyProvider.fromFiles(properties.privateKeyPath, properties.publicKeyPath)

    @Bean
    fun tokenService(keyProvider: KeyProvider, properties: AuthProperties, clock: Clock) =
        TokenService(keyProvider, properties.issuer, properties.audience, properties.accessTokenTtl, clock)

    @Bean
    fun loginThrottle(properties: AuthProperties, clock: Clock) = with(properties.throttle) {
        LoginThrottle(maxFailuresPerUsername, maxFailuresPerIp, window, clock)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
