package hu.bme.aut.auth.persistence

import hu.bme.aut.auth.domain.Avatar
import hu.bme.aut.auth.domain.OutboxEvent
import hu.bme.aut.auth.domain.RefreshToken
import hu.bme.aut.auth.domain.Session
import hu.bme.aut.auth.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface UserRepository : JpaRepository<User, Long> {
    @Query("select u from User u where lower(u.username) = lower(:username)")
    fun findByUsernameIgnoreCase(username: String): User?

    @Query("select count(u) > 0 from User u where lower(u.username) = lower(:username)")
    fun existsByUsernameIgnoreCase(username: String): Boolean

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email)")
    fun existsByEmailIgnoreCase(email: String): Boolean

    fun findByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(prefix: String, pageable: Pageable): Page<User>
}

interface AvatarRepository : JpaRepository<Avatar, Long>

interface SessionRepository : JpaRepository<Session, UUID> {
    @Modifying
    @Query("update Session s set s.revokedAt = :now where s.userId = :userId and s.revokedAt is null")
    fun revokeAllForUser(userId: Long, now: Instant): Int
}

interface RefreshTokenRepository : JpaRepository<RefreshToken, String>

interface OutboxRepository : JpaRepository<OutboxEvent, UUID> {
    /** Locks the oldest unpublished rows; other publisher instances skip them instead of sending them twice. */
    @Query(
        value = "select * from outbox where published_at is null order by created_at limit :limit for update skip locked",
        nativeQuery = true
    )
    fun lockUnpublished(limit: Int): List<OutboxEvent>
}
