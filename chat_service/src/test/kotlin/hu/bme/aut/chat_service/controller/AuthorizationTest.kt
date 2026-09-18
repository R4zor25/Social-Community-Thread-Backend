package hu.bme.aut.chat_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.chat_service.domain.AppUser
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.repositories.ChatRepository
import hu.bme.aut.chat_service.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthorizationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository
) {
    private var aliceId = 0L
    private var bobId = 0L
    private var conversationId = 0L

    @BeforeEach
    fun setUp() {
        aliceId = userRepository.save(AppUser(userName = "alice", email = "alice", password = "pw")).userId
        bobId = userRepository.save(AppUser(userName = "bob", email = "bob", password = "pw")).userId
        mockMvc.post("/api/chat/$aliceId/create") {
            header("X-User-Id", aliceId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ChatConversation(conversationName = "alice's chat"))
        }.andExpect { status { isOk() } }
        conversationId = chatRepository.findAll().single().id!!
    }

    private fun sendAs(userId: Long, message: ChatMessage) =
        mockMvc.post("/api/chat/$userId/$conversationId/send") {
            header("X-User-Id", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(message)
        }

    private fun participantsChangeAs(userId: Long, action: String, userIds: List<Long>) =
        mockMvc.post("/api/chat/$conversationId/$action") {
            header("X-User-Id", userId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(userIds)
        }

    @Test
    fun missingUserIdHeaderIsUnauthorized() {
        mockMvc.get("/api/chat/$aliceId/conversations").andExpect { status { isUnauthorized() } }
        mockMvc.post("/api/chat/$conversationId/addParticipants") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(listOf(bobId))
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun actingAsAnotherUserIsForbidden() {
        mockMvc.get("/api/chat/$aliceId/conversations") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun nonParticipantCannotReadOrWriteTheConversation() {
        mockMvc.get("/api/chat/$bobId/$conversationId") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isForbidden() } }
        sendAs(bobId, ChatMessage(messageText = "hi")).andExpect { status { isForbidden() } }
    }

    @Test
    fun nonParticipantCannotChangeParticipants() {
        participantsChangeAs(bobId, "addParticipants", listOf(bobId)).andExpect { status { isForbidden() } }
        participantsChangeAs(bobId, "removeParticipants", listOf(aliceId)).andExpect { status { isForbidden() } }
    }

    @Test
    fun addedParticipantCanReadTheConversation() {
        participantsChangeAs(aliceId, "addParticipants", listOf(bobId)).andExpect { status { isOk() } }

        mockMvc.get("/api/chat/$bobId/$conversationId") {
            header("X-User-Id", bobId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.chatParticipants.length()") { value(2) }
            jsonPath("$.chatParticipants[0].email") { doesNotExist() }
            jsonPath("$.chatCreator.email") { doesNotExist() }
        }
    }

    @Test
    fun sendingWithAnExistingMessageIdDoesNotOverwriteIt() {
        sendAs(aliceId, ChatMessage(messageText = "first")).andExpect { status { isOk() } }
        val firstId = chatRepository.findById(conversationId).get().messageList.single().id

        sendAs(aliceId, ChatMessage(id = firstId, messageText = "edited")).andExpect { status { isOk() } }

        val texts = chatRepository.findById(conversationId).get().messageList.map { it.messageText }
        assertEquals(listOf("first", "edited"), texts.sorted().reversed())
    }
}
