package hu.bme.aut.auth_service.repositories

import hu.bme.aut.auth_service.domain.AppUser
import hu.bme.aut.auth_service.domain.RefreshToken
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
@Transactional
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long>{

    fun findByToken(token :String): Optional<RefreshToken>
    fun findByUser(user: AppUser) : Optional<RefreshToken>

}