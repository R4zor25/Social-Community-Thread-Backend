package hu.bme.aut.auth_service.services

import hu.bme.aut.auth_service.domain.RefreshToken
import hu.bme.aut.auth_service.repositories.RefreshTokenRepository
import hu.bme.aut.auth_service.repositories.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class RefreshTokenService(
    val refreshTokenRepository: RefreshTokenRepository,
    val userRepository: UserRepository
) {

    fun createRefreshToken(userName : String) : RefreshToken {
        val appUser = userRepository.findByUserName(userName).get()
        val rt = refreshTokenRepository.findByUser(appUser)
        val refreshToken = if(rt.isEmpty) {
             RefreshToken().apply {
                this.user = appUser
                this.token = (UUID.randomUUID().toString())
                this.expiryDate = Instant.now().plusSeconds(60 * 60 * 24) // 1 day
            }
        } else {
            rt.get().apply {
                this.token = (UUID.randomUUID().toString())
                this.expiryDate = Instant.now().plusSeconds(60 * 60 * 24) // 1 day
            }
        }
        return refreshTokenRepository.save(refreshToken)
    }

    fun findByToken(token : String) : Optional<RefreshToken>{
        return refreshTokenRepository.findByToken(token)
    }

    fun verifyExpiration(token : RefreshToken) : RefreshToken{
        if(token.expiryDate < Instant.now()){
            refreshTokenRepository.delete(token)
            throw RuntimeException("${token.token} Refresh token was expired. Please sign in again!")
        } else {
            return token
        }
    }
}