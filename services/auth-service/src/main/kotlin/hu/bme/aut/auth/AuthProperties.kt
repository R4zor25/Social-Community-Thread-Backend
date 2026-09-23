package hu.bme.aut.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path
import java.time.Duration

@ConfigurationProperties("auth")
data class AuthProperties(
    val issuer: String,
    val audience: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
    val privateKeyPath: Path,
    val publicKeyPath: Path,
    val throttle: Throttle,
    val outbox: Outbox
) {
    data class Throttle(val maxFailuresPerUsername: Int, val maxFailuresPerIp: Int, val window: Duration)
    data class Outbox(val pollInterval: Duration, val schedulingEnabled: Boolean)
}
