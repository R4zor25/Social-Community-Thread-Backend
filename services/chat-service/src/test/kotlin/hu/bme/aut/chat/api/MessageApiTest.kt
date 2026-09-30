package hu.bme.aut.chat.api

import hu.bme.aut.chat.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

class MessageApiTest : IntegrationTest() {

    private var conversation = 0L

    @BeforeEach
    fun conversationOfAliceAndBob() {
        createConversation(participantIds = listOf(2))
        conversation = conversationId()
    }

    @Test
    fun participantsSendAndReadMessagesNewestFirst() {
        sendMessage(conversation, alice, "first").andExpect {
            status { isCreated() }
            jsonPath("$.author.username") { value("alice") }
            jsonPath("$.body") { value("first") }
        }
        sendMessage(conversation, bob, "second")

        mockMvc.get("/api/v2/conversations/$conversation/messages") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.items[0].body") { value("second") }
            jsonPath("$.items[1].author.id") { value(1) }
        }
        mockMvc.get("/api/v2/conversations/$conversation/messages?size=1") { with(bob) }.andExpect {
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.totalPages") { value(2) }
        }
    }

    @Test
    fun nonParticipantsCanNeitherSendNorRead() {
        sendMessage(conversation, carol, "let me in").andExpect { status { isForbidden() } }
        mockMvc.get("/api/v2/conversations/$conversation/messages") { with(carol) }.andExpect { status { isForbidden() } }
    }

    @Test
    fun aBlankMessageIs400() {
        sendMessage(conversation, alice, " ").andExpect { status { isBadRequest() } }
    }
}
