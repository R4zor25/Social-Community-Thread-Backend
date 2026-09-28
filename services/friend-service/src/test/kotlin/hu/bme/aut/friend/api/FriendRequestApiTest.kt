package hu.bme.aut.friend.api

import hu.bme.aut.friend.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class FriendRequestApiTest : IntegrationTest() {

    @Test
    fun aRequestIsSentByTheCallerAndHasALocation() {
        val response = sendRequest(alice, 2).andExpect {
            status { isCreated() }
            jsonPath("$.sender.id") { value(1) }
            jsonPath("$.sender.username") { value("alice") }
            jsonPath("$.recipient.username") { value("bob") }
        }.andReturn().response

        assertThat(response.getHeader("Location")).isEqualTo("/api/v2/friend-requests/${requestId()}")
    }

    @Test
    fun aRequestToYourselfIs400() {
        sendRequest(alice, 1).andExpect { status { isBadRequest() } }
    }

    @Test
    fun aRequestToAnUnknownUserIs404() {
        sendRequest(alice, 999).andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("User does not exist") }
        }
    }

    @Test
    fun aSecondRequestInEitherDirectionIs409() {
        sendRequest(alice, 2).andExpect { status { isCreated() } }

        sendRequest(alice, 2).andExpect { status { isConflict() } }
        sendRequest(bob, 1).andExpect { status { isConflict() } }
        assertThat(count("friend_requests")).isEqualTo(1)
    }

    @Test
    fun aRequestBetweenFriendsIs409() {
        sendRequest(alice, 2)
        mockMvc.post("/api/v2/friend-requests/${requestId()}/accept") { with(bob) }.andExpect { status { isNoContent() } }

        sendRequest(bob, 1).andExpect {
            status { isConflict() }
            jsonPath("$.detail") { value("You are already friends") }
        }
    }

    @Test
    fun requestsAreListedByDirection() {
        sendRequest(alice, 2)
        sendRequest(carol, 2)

        mockMvc.get("/api/v2/friend-requests") { with(bob) }.andExpect {
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.items[0].sender.username") { value("carol") }
        }
        mockMvc.get("/api/v2/friend-requests?direction=outgoing") { with(alice) }.andExpect {
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].recipient.username") { value("bob") }
        }
        mockMvc.get("/api/v2/friend-requests?direction=outgoing") { with(bob) }.andExpect { jsonPath("$.totalItems") { value(0) } }
    }

    @Test
    fun onlyTheRecipientCanAcceptAndAcceptingMakesFriends() {
        sendRequest(alice, 2)
        val id = requestId()

        mockMvc.post("/api/v2/friend-requests/$id/accept") { with(alice) }.andExpect { status { isForbidden() } }
        mockMvc.post("/api/v2/friend-requests/$id/accept") { with(carol) }.andExpect { status { isForbidden() } }
        mockMvc.post("/api/v2/friend-requests/$id/accept") { with(bob) }.andExpect { status { isNoContent() } }

        assertThat(count("friend_requests")).isZero()
        assertThat(jdbc.queryForMap("select user_low, user_high from friendships")).isEqualTo(mapOf("user_low" to 1L, "user_high" to 2L))
    }

    @Test
    fun onlyTheRecipientCanDecline() {
        sendRequest(alice, 2)
        val id = requestId()

        mockMvc.post("/api/v2/friend-requests/$id/decline") { with(alice) }.andExpect { status { isForbidden() } }
        mockMvc.post("/api/v2/friend-requests/$id/decline") { with(bob) }.andExpect { status { isNoContent() } }

        assertThat(count("friend_requests")).isZero()
        assertThat(count("friendships")).isZero()
    }

    @Test
    fun onlyTheSenderCanRevoke() {
        sendRequest(alice, 2)
        val id = requestId()

        mockMvc.delete("/api/v2/friend-requests/$id") { with(bob) }.andExpect { status { isForbidden() } }
        mockMvc.delete("/api/v2/friend-requests/$id") { with(alice) }.andExpect { status { isNoContent() } }

        assertThat(count("friend_requests")).isZero()
    }

    @Test
    fun unknownRequestsAre404() {
        mockMvc.post("/api/v2/friend-requests/999/accept") { with(bob) }.andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("Friend request does not exist") }
        }
    }

    @Test
    fun responsesContainNoEmail() {
        sendRequest(alice, 2)
        val body = mockMvc.get("/api/v2/friend-requests") { with(bob) }.andReturn().response.contentAsString
        assertThat(body).doesNotContain("email")
    }
}
