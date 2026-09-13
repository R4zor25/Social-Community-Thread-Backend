package hu.bme.aut.auth_service.services

import hu.bme.aut.auth_service.domain.RefreshToken
import hu.bme.aut.auth_service.repositories.RefreshTokenRepository
import hu.bme.aut.auth_service.repositories.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.*

@Service
class RefreshTokenService(
    val refreshTokenRepository: RefreshTokenRepository,
    val userRepository: UserRepository,
    @Value("\${jwt.refresh-token-ttl}") private val refreshTokenTtl: Duration
) {

    fun createRefreshToken(userName : String) : RefreshToken {
        val appUser = userRepository.findByUserName(userName).get()
        val rt = refreshTokenRepository.findByUser(appUser)
        val refreshToken = if(rt.isEmpty) {
             RefreshToken().apply {
                this.user = appUser
                this.token = (UUID.randomUUID().toString())
                this.expiryDate = Instant.now().plus(refreshTokenTtl)
            }
        } else {
            rt.get().apply {
                this.token = (UUID.randomUUID().toString())
                this.expiryDate = Instant.now().plus(refreshTokenTtl)
            }
        }
        return refreshTokenRepository.save(refreshToken)
    }

    fun findByToken(token : String) : Optional<RefreshToken>{
        return refreshTokenRepository.findByToken(token)
    }

    /** Returns the token if it is still valid; deletes it and returns null if it has expired. */
    fun verifyExpiration(token : RefreshToken) : RefreshToken? {
        if(token.expiryDate < Instant.now()){
            refreshTokenRepository.delete(token)
            return null
        }
        return token
    }
}