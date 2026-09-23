package hu.bme.aut.auth.domain

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Clock
import java.time.Duration
import java.util.UUID

data class IssuedToken(val value: String, val expiresIn: Duration)

class TokenService(
    private val keyProvider: KeyProvider,
    private val issuer: String,
    private val audience: String,
    private val accessTokenTtl: Duration,
    private val clock: Clock
) {
    private val encoder = NimbusJwtEncoder(ImmutableJWKSet(JWKSet(keyProvider.rsaKey)))

    fun issue(userId: Long, username: String): IssuedToken {
        val now = clock.instant()
        val claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(listOf(audience))
            .subject(userId.toString())
            .claim("preferred_username", username)
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenTtl))
            .id(UUID.randomUUID().toString())
            .build()
        val header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyProvider.rsaKey.keyID).build()
        return IssuedToken(encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue, accessTokenTtl)
    }
}
