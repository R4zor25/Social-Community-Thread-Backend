package hu.bme.aut.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false) var username: String,
    @Column(nullable = false) var email: String,
    @Column(name = "password_hash", nullable = false) var passwordHash: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
)

/** Kept apart from `users` so that loading a user never loads the image. */
@Entity
@Table(name = "user_avatars")
class Avatar(
    @Id @Column(name = "user_id") val userId: Long,
    @Column(nullable = false) var content: ByteArray,
    @Column(name = "content_type", nullable = false) var contentType: String
)

@Entity
@Table(name = "sessions")
class Session(
    @Id val id: UUID,
    @Column(name = "user_id", nullable = false) val userId: Long,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "revoked_at") var revokedAt: Instant? = null
)

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id @Column(name = "token_hash") val tokenHash: String,
    @Column(name = "session_id", nullable = false) val sessionId: UUID,
    @Column(name = "expires_at", nullable = false) val expiresAt: Instant,
    @Column(name = "used_at") var usedAt: Instant? = null
)

@Entity
@Table(name = "outbox")
class OutboxEvent(
    @Id val id: UUID,
    @Column(name = "aggregate_id", nullable = false) val aggregateId: String,
    @Column(nullable = false) val type: String,
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false) val payload: String,
    @Column(name = "created_at", nullable = false) val createdAt: Instant,
    @Column(name = "published_at") var publishedAt: Instant? = null
)
