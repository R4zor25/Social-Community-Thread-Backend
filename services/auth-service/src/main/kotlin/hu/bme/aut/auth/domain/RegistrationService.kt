package hu.bme.aut.auth.domain

import hu.bme.aut.auth.persistence.OutboxRepository
import hu.bme.aut.auth.persistence.UserRepository
import hu.bme.aut.common.web.error.ConflictException
import hu.bme.aut.events.EventEnvelope
import hu.bme.aut.events.EventJson
import hu.bme.aut.events.UserRegistered
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class RegistrationService(
    private val users: UserRepository,
    private val outbox: OutboxRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) {
    /**
     * Creates the user and its UserRegistered event in one transaction. The unique indexes catch
     * concurrent registrations that pass the checks below; they surface as a 409 as well.
     */
    @Transactional
    fun register(username: String, email: String, password: String): User {
        if (users.existsByUsernameIgnoreCase(username)) throw ConflictException("Username is already taken")
        if (users.existsByEmailIgnoreCase(email)) throw ConflictException("Email address is already registered")

        val now = clock.instant()
        val user = users.saveAndFlush(User(username, email, requireNotNull(passwordEncoder.encode(password)), now))
        val userId = requireNotNull(user.id)
        val event = EventEnvelope(UUID.randomUUID(), UserRegistered.TYPE, UserRegistered.VERSION, now, UserRegistered(userId, username))
        outbox.save(OutboxEvent(event.eventId, userId.toString(), event.type, EventJson.write(event), now))
        return user
    }
}
