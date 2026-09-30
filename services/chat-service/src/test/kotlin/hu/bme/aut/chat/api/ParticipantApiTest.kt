package hu.bme.aut.chat.api

import hu.bme.aut.chat.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor

class ParticipantApiTest : IntegrationTest() {

    private var conversation = 0L

    @BeforeEach
    fun conversationOfAliceAndBob() {
        createConversation(participantIds = listOf(2))
        conversation = conversationId()
    }

    private fun add(as_: RequestPostProcessor, vararg userIds: Long) = mockMvc.post("/api/v2/conversations/$conversation/participants") {
        with(as_)
        contentType = MediaType.APPLICATION_JSON
        content = """{"userIds":${userIds.joinToString(",", "[", "]")}}"""
    }

    private fun remove(as_: RequestPostProcessor, userId: Long) =
        mockMvc.delete("/api/v2/conversations/$conversation/participants/$userId") { with(as_) }

    @Test
    fun participantsAddOthersIdempotently() {
        repeat(2) { add(bob, 3, 2).andExpect { status { isNoContent() } } }

        assertThat(participants(conversation)).containsExactly(1, 2, 3)
    }

    @Test
    fun nonParticipantsCannotAddAndUnknownUsersAre404() {
        add(carol, 3).andExpect { status { isForbidden() } }
        add(alice, 999).andExpect { status { isNotFound() } }
        assertThat(participants(conversation)).containsExactly(1, 2)
    }

    @Test
    fun anyoneCanLeave() {
        remove(bob, 2).andExpect { status { isNoContent() } }

        assertThat(participants(conversation)).containsExactly(1)
    }

    @Test
    fun onlyTheCreatorRemovesOthers() {
        add(alice, 3)

        remove(bob, 3).andExpect { status { isForbidden() } }
        remove(bob, 1).andExpect { status { isForbidden() } }
        remove(alice, 3).andExpect { status { isNoContent() } }

        assertThat(participants(conversation)).containsExactly(1, 2)
    }

    @Test
    fun removingSomeoneWhoIsNotInItIsANoOp() {
        remove(alice, 3).andExpect { status { isNoContent() } }
        assertThat(participants(conversation)).containsExactly(1, 2)
    }

    @Test
    fun whenTheLastParticipantLeavesTheConversationIsDeleted() {
        sendMessage(conversation, bob, "bye")
        remove(bob, 2)
        remove(alice, 1).andExpect { status { isNoContent() } }

        assertThat(count("conversations")).isZero()
        assertThat(count("messages")).isZero()
        assertThat(count("user_projection")).isEqualTo(3)
    }
}
