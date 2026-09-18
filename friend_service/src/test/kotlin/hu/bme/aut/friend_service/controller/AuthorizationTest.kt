package hu.bme.aut.friend_service.controller

import hu.bme.aut.friend_service.domain.AppUser
import hu.bme.aut.friend_service.repositories.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthorizationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val userRepository: UserRepository
) {
    private var aliceId = 0L
    private var bobId = 0L

    @BeforeEach
    fun setUp() {
        aliceId = userRepository.save(AppUser(userName = "alice", email = "alice", password = "pw")).userId
        bobId = userRepository.save(AppUser(userName = "bob", email = "bob", password = "pw")).userId
    }

    @Test
    fun missingUserIdHeaderIsUnauthorized() {
        mockMvc.get("/api/friend/$aliceId").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun actingAsAnotherUserIsForbidden() {
        mockMvc.post("/api/friend/$aliceId/send/$bobId") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun acceptingWithoutAPendingRequestFails() {
        mockMvc.post("/api/friend/$aliceId/accept/$bobId") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isNotFound() } }

        mockMvc.get("/api/friend/$aliceId") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isOk() }; jsonPath("$.size()") { value(0) } }
    }

    @Test
    fun sendingAFriendRequestToYourselfIsRejected() {
        mockMvc.post("/api/friend/$aliceId/send/$aliceId") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun friendListDoesNotContainEmails() {
        mockMvc.post("/api/friend/$aliceId/send/$bobId") { header("X-User-Id", aliceId.toString()) }
            .andExpect { status { isOk() } }
        mockMvc.post("/api/friend/$bobId/accept/$aliceId") { header("X-User-Id", bobId.toString()) }
            .andExpect { status { isOk() } }

        mockMvc.get("/api/friend/$aliceId") {
            header("X-User-Id", aliceId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].userName") { value("bob") }
            jsonPath("$[0].email") { doesNotExist() }
        }
    }
}
