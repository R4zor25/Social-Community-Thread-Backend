package hu.bme.aut.chat.api

import hu.bme.aut.chat.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class ConversationApiTest : IntegrationTest() {

    @Test
    fun theCreatorIsAlwaysAParticipantAndDuplicatesCollapse() {
        val response = createConversation(participantIds = listOf(2, 2, 1)).andExpect {
            status { isCreated() }
            jsonPath("$.creator.username") { value("alice") }
            jsonPath("$.participants.length()") { value(2) }
        }.andReturn().response

        val id = conversationId()
        assertThat(response.getHeader("Location")).isEqualTo("/api/v2/conversations/$id")
        assertThat(participants(id)).containsExactly(1, 2)
    }

    @Test
    fun serverOwnedFieldsInTheBodyAreIgnored() {
        mockMvc.post("/api/v2/conversations") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Chat","participantIds":[2],"id":77,"creatorId":3,"createdAt":"1970-01-01T00:00:00Z"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.creator.id") { value(1) }
        }
        assertThat(conversationId()).isNotEqualTo(77)
    }

    @Test
    fun unknownParticipantsAre404AndNothingIsCreated() {
        createConversation(participantIds = listOf(2, 999)).andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("User does not exist") }
        }
        assertThat(count("conversations")).isZero()
    }

    @Test
    fun onlyParticipantsCanSeeAConversation() {
        createConversation(participantIds = listOf(2))
        val id = conversationId()

        mockMvc.get("/api/v2/conversations/$id") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Chat") }
        }
        mockMvc.get("/api/v2/conversations/$id") { with(carol) }.andExpect { status { isForbidden() } }
        mockMvc.get("/api/v2/conversations/999") { with(carol) }.andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("Conversation does not exist") }
        }
    }

    @Test
    fun theListShowsOwnConversationsByLatestMessage() {
        createConversation(name = "First", participantIds = listOf(2))
        createConversation(name = "Second", participantIds = listOf(2))
        createConversation(as_ = carol, name = "Not mine", participantIds = listOf(1))
        sendMessage(conversationId("First"), bob, "bump").andExpect { status { isCreated() } }

        mockMvc.get("/api/v2/conversations") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.items[0].name") { value("First") }
            jsonPath("$.items[1].name") { value("Second") }
        }
    }

    @Test
    fun imageIsForParticipantsOnly() {
        createConversation(participantIds = listOf(2))
        val id = conversationId()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        mockMvc.put("/api/v2/conversations/$id/image") { with(carol); contentType = MediaType.IMAGE_PNG; content = png }
            .andExpect { status { isForbidden() } }
        mockMvc.put("/api/v2/conversations/$id/image") { with(bob); contentType = MediaType.TEXT_PLAIN; content = "x" }
            .andExpect { status { isUnsupportedMediaType() } }
        mockMvc.put("/api/v2/conversations/$id/image") { with(bob); contentType = MediaType.IMAGE_PNG; content = ByteArray(5 * 1024 * 1024 + 1) }
            .andExpect { status { isContentTooLarge() } }
        mockMvc.put("/api/v2/conversations/$id/image") { with(bob); contentType = MediaType.IMAGE_PNG; content = png }
            .andExpect { status { isNoContent() } }

        mockMvc.get("/api/v2/conversations/$id/image") { with(carol) }.andExpect { status { isForbidden() } }
        val bytes = mockMvc.get("/api/v2/conversations/$id/image") { with(alice) }.andExpect { status { isOk() } }.andReturn().response.contentAsByteArray
        assertThat(bytes).isEqualTo(png)
        mockMvc.get("/api/v2/conversations/$id") { with(alice) }.andExpect { jsonPath("$.hasImage") { value(true) } }
    }

    @Test
    fun responsesContainNoEmail() {
        createConversation(participantIds = listOf(2))
        val id = conversationId()
        sendMessage(id, bob, "hi")

        listOf("/api/v2/conversations", "/api/v2/conversations/$id", "/api/v2/conversations/$id/messages").forEach { path ->
            assertThat(mockMvc.get(path) { with(alice) }.andReturn().response.contentAsString).describedAs(path).doesNotContain("email")
        }
    }
}
