package hu.bme.aut.apigateway.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key


@Component
class JwtUtil(
    @Value("\${jwt.secret}") private val secret: String
) {
    fun validateToken(token: String?) {
        Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token)
    }

    private val signKey: Key
        private get() {
            val keyBytes: ByteArray = Decoders.BASE64.decode(secret)
            return Keys.hmacShaKeyFor(keyBytes)
        }
}