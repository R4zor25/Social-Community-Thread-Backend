package hu.bme.aut.apigateway.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key


@Component
class JwtUtil(
    @Value("\${jwt.secret}") secret: String
) {
    private val signKey: Key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))

    /**
     * Verifies signature and expiry. Throws [io.jsonwebtoken.JwtException]
     * or [IllegalArgumentException] if the token is not acceptable.
     */
    fun parseClaims(token: String): Claims =
        Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token).body
}
