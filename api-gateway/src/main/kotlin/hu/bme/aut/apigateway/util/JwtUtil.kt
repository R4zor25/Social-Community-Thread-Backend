package hu.bme.aut.apigateway.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key


@Component
class JwtUtil {
    fun validateToken(token: String?) {
        Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token)
    }

    private val signKey: Key
        private get() {
            val keyBytes: ByteArray = Decoders.BASE64.decode(SECRET)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    companion object {
        const val SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"
    }
}