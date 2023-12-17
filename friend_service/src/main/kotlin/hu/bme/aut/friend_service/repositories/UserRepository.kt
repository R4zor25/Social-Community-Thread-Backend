package hu.bme.aut.friend_service.repositories

import hu.bme.aut.friend_service.domain.AppUser
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository

@Transactional
interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUserName(userName: String): List<AppUser>
    fun findByEmail(email: String): List<AppUser>

}