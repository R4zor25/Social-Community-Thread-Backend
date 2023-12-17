package hu.bme.aut.auth_service.repositories

import hu.bme.aut.auth_service.domain.AppUser
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Transactional
interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUserName(userName: String): Optional<AppUser>
    fun findByEmail(email: String): Optional<AppUser>
}