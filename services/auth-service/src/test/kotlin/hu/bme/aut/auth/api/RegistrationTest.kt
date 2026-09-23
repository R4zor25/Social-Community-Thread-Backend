package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.events.EventJson
import hu.bme.aut.events.UserRegistered
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

class RegistrationTest : IntegrationTest() {

    private fun register(username: String, email: String, password: String = "correct horse"): ResultActionsDsl =
        mockMvc.post("/api/v2/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"$username","email":"$email","password":"$password"}"""
        }

    @Test
    fun registrationCreatesTheUserAndAnOutboxEvent() {
        val result = register("alice", "alice@example.com").andExpect {
            status { isCreated() }
            jsonPath("$.username") { value("alice") }
            jsonPath("$.email") { doesNotExist() }
        }.andReturn()

        val id = jdbc.queryForObject("select id from users where username = 'alice'", Long::class.java)
        assertThat(result.response.getHeader("Location")).isEqualTo("/api/v2/users/$id")

        val (type, aggregateId, payload) = jdbc.queryForMap("select type, aggregate_id, payload::text as payload from outbox").let {
            Triple(it["type"], it["aggregate_id"], it["payload"] as String)
        }
        assertThat(type).isEqualTo(UserRegistered.TYPE)
        assertThat(aggregateId).isEqualTo(id.toString())
        assertThat(EventJson.read(payload, UserRegistered::class.java).payload).isEqualTo(UserRegistered(id!!, "alice"))
    }

    @Test
    fun passwordsAreStoredAsBcryptHashes() {
        register("alice", "alice@example.com", password = "correct horse")

        val hash = jdbc.queryForObject("select password_hash from users", String::class.java)
        assertThat(hash).startsWith("{bcrypt}").doesNotContain("correct horse")
    }

    @ParameterizedTest
    @CsvSource(
        "ab, alice@example.com, correct horse, username",
        "'al ice', alice@example.com, correct horse, username",
        "abcdefghijklmnopqrstuvwxyz0123456, alice@example.com, correct horse, username",
        "alice, not-an-email, correct horse, email",
        "alice, alice@example.com, short, password"
    )
    fun invalidRegistrationsNameTheField(username: String, email: String, password: String, field: String) {
        register(username, email, password).andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value(field) }
        }
        assertThat(jdbc.queryForObject("select count(*) from users", Long::class.java)).isZero()
    }

    @Test
    fun takenUsernameIsAConflictIgnoringCase() {
        register("alice", "alice@example.com").andExpect { status { isCreated() } }

        register("ALICE", "other@example.com").andExpect { status { isConflict() } }
    }

    @Test
    fun takenEmailIsAConflictIgnoringCase() {
        register("alice", "alice@example.com").andExpect { status { isCreated() } }

        register("bob", "ALICE@example.com").andExpect { status { isConflict() } }
    }
}
