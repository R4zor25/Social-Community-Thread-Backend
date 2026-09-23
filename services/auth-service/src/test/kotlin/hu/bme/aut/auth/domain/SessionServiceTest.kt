package hu.bme.aut.auth.domain

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.common.web.error.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.Duration

class SessionServiceTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    @Autowired
    lateinit var sessions: SessionService

    private val ip = "203.0.113.1"

    @BeforeEach
    fun registerAlice() {
        registration.register("alice", "alice@example.com", "correct horse")
    }

    private fun assertUnauthorized(block: () -> Unit) =
        assertThatThrownBy(block).isInstanceOfSatisfying(ApiException::class.java) {
            assertThat(it.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

    @Test
    fun aRotatedTokenCannotBeUsedAgain() {
        val first = sessions.login("alice", "correct horse", ip).refreshToken
        sessions.refresh(first)

        assertUnauthorized { sessions.refresh(first) }
    }

    @Test
    fun reusingARotatedTokenRevokesEverySessionOfTheUser() {
        val phone = sessions.login("alice", "correct horse", ip).refreshToken
        val laptop = sessions.login("alice", "correct horse", ip).refreshToken
        val phoneRotated = sessions.refresh(phone).refreshToken

        assertUnauthorized { sessions.refresh(phone) }

        assertUnauthorized { sessions.refresh(phoneRotated) }
        assertUnauthorized { sessions.refresh(laptop) }
    }

    @Test
    fun loginsOnTwoDevicesAreIndependent() {
        val phone = sessions.login("alice", "correct horse", ip).refreshToken
        val laptop = sessions.login("alice", "correct horse", ip).refreshToken

        sessions.logout(phone)

        assertUnauthorized { sessions.refresh(phone) }
        assertThat(sessions.refresh(laptop).refreshToken).isNotBlank()
    }

    @Test
    fun refreshAfterLogoutIs401() {
        val token = sessions.login("alice", "correct horse", ip).refreshToken
        sessions.logout(token)

        assertUnauthorized { sessions.refresh(token) }
    }

    @Test
    fun expiredRefreshTokenIs401() {
        val token = sessions.login("alice", "correct horse", ip).refreshToken
        clock.advance(Duration.ofDays(7).plusSeconds(1))

        assertUnauthorized { sessions.refresh(token) }
    }

    @Test
    fun refreshTokensAreStoredOnlyAsHashes() {
        val token = sessions.login("alice", "correct horse", ip).refreshToken

        val stored = jdbc.queryForList("select token_hash from refresh_tokens", String::class.java)
        assertThat(stored).hasSize(1).doesNotContain(token)
        assertThat(stored.single()).hasSize(64)
    }
}
