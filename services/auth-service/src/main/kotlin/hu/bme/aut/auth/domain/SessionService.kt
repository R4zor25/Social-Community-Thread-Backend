package hu.bme.aut.auth.domain

import hu.bme.aut.auth.AuthProperties
import hu.bme.aut.auth.persistence.RefreshTokenRepository
import hu.bme.aut.auth.persistence.SessionRepository
import hu.bme.aut.auth.persistence.UserRepository
import hu.bme.aut.common.web.error.ApiException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

class InvalidCredentialsException : ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password")
class InvalidRefreshTokenException : ApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token")

data class Tokens(val accessToken: IssuedToken, val refreshToken: String)

/** One session per login; its refresh token is rotated on every refresh and stored only as a SHA-256 hash. */
@Service
class SessionService(
    private val users: UserRepository,
    private val sessions: SessionRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val tokenService: TokenService,
    private val passwordEncoder: PasswordEncoder,
    private val throttle: LoginThrottle,
    private val properties: AuthProperties,
    private val clock: Clock
) {
    private val random = SecureRandom()

    // Unknown users still cost one hash comparison, so response times do not reveal which usernames exist.
    private val unknownUserHash = requireNotNull(passwordEncoder.encode(UUID.randomUUID().toString()))

    @Transactional
    fun login(username: String, password: String, clientIp: String): Tokens {
        throttle.check(username, clientIp)
        val user = users.findByUsernameIgnoreCase(username)
        val matches = passwordEncoder.matches(password, user?.passwordHash ?: unknownUserHash)
        if (user == null || !matches) {
            throttle.recordFailure(username, clientIp)
            throw InvalidCredentialsException()
        }
        throttle.recordSuccess(username)
        val session = sessions.save(Session(UUID.randomUUID(), requireNotNull(user.id), clock.instant()))
        return issue(user, session)
    }

    /** A token that was already used means it leaked: every session of its user is revoked. */
    @Transactional(noRollbackFor = [InvalidRefreshTokenException::class])
    fun refresh(refreshToken: String): Tokens {
        val now = clock.instant()
        val stored = refreshTokens.findByIdOrNull(hash(refreshToken)) ?: throw InvalidRefreshTokenException()
        val session = sessions.findByIdOrNull(stored.sessionId) ?: throw InvalidRefreshTokenException()
        if (stored.usedAt != null) {
            sessions.revokeAllForUser(session.userId, now)
            throw InvalidRefreshTokenException()
        }
        if (session.revokedAt != null || !stored.expiresAt.isAfter(now)) throw InvalidRefreshTokenException()

        stored.usedAt = now
        val user = users.findByIdOrNull(session.userId) ?: throw InvalidRefreshTokenException()
        return issue(user, session)
    }

    @Transactional
    fun logout(refreshToken: String) {
        val stored = refreshTokens.findByIdOrNull(hash(refreshToken)) ?: return
        val session = sessions.findByIdOrNull(stored.sessionId) ?: return
        if (session.revokedAt == null) session.revokedAt = clock.instant()
    }

    private fun issue(user: User, session: Session): Tokens {
        val raw = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        refreshTokens.save(RefreshToken(hash(raw), session.id, clock.instant().plus(properties.refreshTokenTtl)))
        return Tokens(tokenService.issue(requireNotNull(user.id), user.username), raw)
    }

    private fun hash(token: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
