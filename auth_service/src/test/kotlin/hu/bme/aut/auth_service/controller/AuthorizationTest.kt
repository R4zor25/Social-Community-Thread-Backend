package hu.bme.aut.auth_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.auth_service.domain.AppUser
import hu.bme.aut.auth_service.repositories.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthorizationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val userRepository: UserRepository
) {
    private var aliceId = 0L
    private var bobId = 0L

    @BeforeEach
    fun setUp() {
        aliceId = userRepository.save(AppUser(userName = "alice", email = "alice", password = "pw")).userId
        bobId = userRepository.save(AppUser(userName = "bob", email = "bob", password = "pw")).userId
    }

    private fun updateProfileImage(targetId: Long, actingUserId: Long) =
        mockMvc.put("/api/auth/users/$targetId/update") {
            header("X-User-Id", actingUserId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AppUser(profileImage = byteArrayOf(1, 2, 3)))
        }

    @Test
    fun missingUserIdHeaderIsUnauthorized() {
        mockMvc.get("/api/auth/users").andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/auth/users/$aliceId").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun authenticatedUserCanLookUpOtherUsers() {
        mockMvc.get("/api/auth/users/$aliceId") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isOk() } }
    }

    @Test
    fun updatingAnotherUsersProfileIsForbidden() {
        updateProfileImage(targetId = aliceId, actingUserId = bobId).andExpect { status { isForbidden() } }
    }

    @Test
    fun updatingOwnProfileIsAllowed() {
        updateProfileImage(targetId = aliceId, actingUserId = aliceId).andExpect { status { isOk() } }
    }

    @Test
    fun updatingANonExistentUserIsNotFound() {
        updateProfileImage(targetId = 999, actingUserId = 999).andExpect {
            status { isNotFound() }
            content { string("User does not exist!") }
        }
    }
}
