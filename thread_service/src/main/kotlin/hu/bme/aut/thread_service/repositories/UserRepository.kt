package hu.bme.aut.thread_service.repositories

import hu.bme.aut.thread_service.models.entities.AppUser
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<AppUser, Long> {
    fun findByUserName(userName: String): List<AppUser>
    fun findByEmail(email: String): List<AppUser>

}