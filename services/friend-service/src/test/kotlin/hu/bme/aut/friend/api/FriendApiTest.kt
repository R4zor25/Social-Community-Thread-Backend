package hu.bme.aut.friend.api

import hu.bme.aut.friend.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor

class FriendApiTest : IntegrationTest() {

    private fun befriend(from: RequestPostProcessor, recipientId: Long, recipient: RequestPostProcessor) {
        sendRequest(from, recipientId)
        val id = jdbc.queryForObject("select max(id) from friend_requests", Long::class.java)
        mockMvc.post("/api/v2/friend-requests/$id/accept") { with(recipient) }.andExpect { status { isNoContent() } }
    }

    @Test
    fun bothSidesSeeTheFriendshipNewestFirst() {
        befriend(alice, 2, bob)
        befriend(carol, 1, alice)

        mockMvc.get("/api/v2/friends") { with(alice) }.andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.items[0].user.username") { value("carol") }
            jsonPath("$.items[1].user.username") { value("bob") }
        }
        mockMvc.get("/api/v2/friends") { with(bob) }.andExpect {
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].user.id") { value(1) }
        }
    }

    @Test
    fun theListIsPagedAndHasNoEmail() {
        befriend(alice, 2, bob)
        befriend(alice, 3, carol)

        val body = mockMvc.get("/api/v2/friends?size=1") { with(alice) }.andExpect {
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.totalPages") { value(2) }
        }.andReturn().response.contentAsString
        assertThat(body).doesNotContain("email")
    }

    @Test
    fun unfriendingRemovesItForBothAndIsIdempotent() {
        befriend(alice, 2, bob)

        repeat(2) { mockMvc.delete("/api/v2/friends/1") { with(bob) }.andExpect { status { isNoContent() } } }

        assertThat(count("friendships")).isZero()
        mockMvc.get("/api/v2/friends") { with(alice) }.andExpect { jsonPath("$.totalItems") { value(0) } }
    }
}
