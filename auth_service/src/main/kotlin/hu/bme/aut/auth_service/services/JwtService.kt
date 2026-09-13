package hu.bme.aut.auth_service.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Duration
import java.util.*


@Component
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-ttl}") private val accessTokenTtl: Duration
) {
    fun validateToken(token: String?) {
        Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token)
    }

    fun generateToken(userId: Long, userName: String): String {
        val claims: Map<String, Any?> = mapOf("userId" to userId)
        return createToken(claims, userName)
    }

    private fun createToken(claims: Map<String, Any?>, userName: String): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userName)
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + accessTokenTtl.toMillis()))
            .signWith(signKey, SignatureAlgorithm.HS256).compact()
    }

    private val signKey: Key
        private get() {
            val keyBytes = Decoders.BASE64.decode(secret)
            return Keys.hmacShaKeyFor(keyBytes)
        }
}
