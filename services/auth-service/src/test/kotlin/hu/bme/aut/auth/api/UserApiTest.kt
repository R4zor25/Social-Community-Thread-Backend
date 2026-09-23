package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import hu.bme.aut.auth.domain.RegistrationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

class UserApiTest : IntegrationTest() {

    @Autowired
    lateinit var registration: RegistrationService

    private var aliceId = 0L
    private var bobId = 0L

    @BeforeEach
    fun registerUsers() {
        aliceId = registration.register("alice", "alice@example.com", "correct horse").id!!
        bobId = registration.register("bob", "bob@example.com", "correct horse").id!!
        registration.register("Alfred", "alfred@example.com", "correct horse")
    }

    private fun asUser(id: Long, username: String) = jwt().jwt { it.subject(id.toString()).claim("preferred_username", username) }

    @Test
    fun meIncludesTheEmail() {
        mockMvc.get("/api/v2/users/me") { with(asUser(aliceId, "alice")) }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(aliceId) }
            jsonPath("$.username") { value("alice") }
            jsonPath("$.email") { value("alice@example.com") }
        }
    }

    @Test
    fun otherUsersAreShownWithoutEmail() {
        mockMvc.get("/api/v2/users/$bobId") { with(asUser(aliceId, "alice")) }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("bob") }
            jsonPath("$.email") { doesNotExist() }
        }
    }

    @Test
    fun unknownUserIs404() {
        mockMvc.get("/api/v2/users/999999") { with(asUser(aliceId, "alice")) }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("User does not exist") }
        }
    }

    @Test
    fun searchIsPagedCaseInsensitiveAndWithoutEmails() {
        mockMvc.get("/api/v2/users?username=AL&size=1") { with(asUser(aliceId, "alice")) }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].username") { value("alfred".replaceFirstChar { it.uppercase() }) }
            jsonPath("$.items[0].email") { doesNotExist() }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.size") { value(1) }
        }
    }

    @Test
    fun avatarRoundTrip() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 1, 2, 3)
        mockMvc.put("/api/v2/users/me/avatar") {
            with(asUser(aliceId, "alice"))
            contentType = MediaType.IMAGE_PNG
            content = png
        }.andExpect { status { isNoContent() } }

        val response = mockMvc.get("/api/v2/users/$aliceId/avatar") { with(asUser(bobId, "bob")) }
            .andExpect { status { isOk() } }.andReturn().response

        // MockMvc adds the default charset to every response; compare type and subtype only.
        assertThat(MediaType.parseMediaType(response.contentType!!).let { "${it.type}/${it.subtype}" }).isEqualTo("image/png")
        assertThat(response.contentAsByteArray).isEqualTo(png)
    }

    @Test
    fun nonImageAvatarIs415() {
        mockMvc.put("/api/v2/users/me/avatar") {
            with(asUser(aliceId, "alice"))
            contentType = MediaType.TEXT_PLAIN
            content = "not an image"
        }.andExpect { status { isUnsupportedMediaType() } }
    }

    @Test
    fun avatarOverFiveMegabytesIs413() {
        mockMvc.put("/api/v2/users/me/avatar") {
            with(asUser(aliceId, "alice"))
            contentType = MediaType.IMAGE_PNG
            content = ByteArray(5 * 1024 * 1024 + 1)
        }.andExpect { status { isContentTooLarge() } }
    }

    @Test
    fun missingAvatarIs404() {
        mockMvc.get("/api/v2/users/$bobId/avatar") { with(asUser(aliceId, "alice")) }.andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("User has no avatar") }
        }
    }
}
