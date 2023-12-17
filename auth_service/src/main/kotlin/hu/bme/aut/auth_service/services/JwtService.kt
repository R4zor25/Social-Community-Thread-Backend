package hu.bme.aut.auth_service.services

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*


@Component
class JwtService {
    fun validateToken(token: String?) {
        Jwts.parserBuilder().setSigningKey(signKey).build().parseClaimsJws(token)
    }

    fun generateToken(userName: String): String {
        val claims: Map<String, Any?> = HashMap()
        return createToken(claims, userName)
    }

    private fun createToken(claims: Map<String, Any?>, userName: String): String {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userName)
            .setIssuedAt(Date(System.currentTimeMillis()))
            .setExpiration(Date(System.currentTimeMillis() + 1000 * 60 * 60 * 60))
            .signWith(signKey, SignatureAlgorithm.HS256).compact()
    }

    private val signKey: Key
        private get() {
            val keyBytes = Decoders.BASE64.decode(SECRET)
            return Keys.hmacShaKeyFor(keyBytes)
        }

    companion object {
        const val SECRET = "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437"
    }
}