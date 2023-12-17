package hu.bme.aut.friend_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.friend_service.domain.AppUser
import hu.bme.aut.friend_service.repositories.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FriendControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
) {
    @TestConfiguration
    internal class DatabasePreparation @Autowired constructor(
        val userRepository: UserRepository
    ) {
        @EventListener(ApplicationStartedEvent::class)
        fun preparedDbForTheTest() {

            val user1 = AppUser(userName = "test1", email = "test1", password = "test")
            val user2 = AppUser(userName = "test2", email = "test2", password = "test")
            val user3 = AppUser(userName = "test3", email = "test3", password = "test")
            val user4 = AppUser(userName = "test4", email = "test3", password = "test")
            user1.friends.add(user2)
            user2.friends.add(user1)
            user1.outgoingFriendRequests.add(user3)
            user3.incomingFriendRequests.add(user1)
            user3.outgoingFriendRequests.add(user2)
            user2.incomingFriendRequests.add(user3)
            userRepository.save(user1)
            userRepository.save(user2)
            userRepository.save(user3)
            userRepository.save(user4)
        }
    }

    @Test
    fun testGetUsersAllFriendSuccessful() {

        val url = "/api/friend/1"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(1) }
            }
        }
    }

    @Test
    fun testGetUsersAllFriendUnsuccessful() {
        val url = "/api/friend/100"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testGetUsersIncomingFriendRequestsSuccessful() {
        val url = "/api/friend/2/incoming"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(1) }
            }
        }
    }

    @Test
    fun testGetUsersIncomingFriendRequestsUnsuccessful() {
        val url = "/api/friend/100/incoming"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testGetUsersOutgoingFriendRequestsSuccessful() {
        val url = "/api/friend/1/outgoing"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(1) }
            }
        }
    }

    @Test
    fun testGetUsersOutgoingFriendRequestsUnsuccessful() {
        val url = "/api/friend/100/outgoing"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testSendFriendRequestSuccessful() {
        val url = "/api/friend/1/send/4"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isOk()
            }
        }

        val getUrl = "/api/friend/1/outgoing"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun testSendFriendRequestUnsuccessful() {
        val url = "/api/friend/100/4"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

        val getUrl = "/api/friend/5/2"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testAcceptFriendRequestSuccessful() {
        val url = "/api/friend/2/accept/3"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isOk()
            }
        }

        val getUrl = "/api/friend/2"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(2) }
            }
        }
    }

    @Test
    fun testAcceptFriendRequestUnsuccessful() {
        val url = "/api/friend/2/accept/100"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

        val postUrl = "/api/friend/100/accept/2"
        val post1 = mockMvc.post(postUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        post1.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testDeclineFriendRequestSuccessful() {
        val url = "/api/friend/2/decline/3"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isOk()
            }
        }

        val getUrl = "/api/friend/2/incoming"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(0) }
            }
        }
    }

    @Test
    fun testDeclineFriendRequestUnsuccessful() {
        val url = "/api/friend/2/decline/100"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

        val postUrl = "/api/friend/100/decline/2"
        val post1 = mockMvc.post(postUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        post1.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testRevokeFriendRequestSuccessful() {
        val url = "/api/friend/1/revoke/3"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isOk()
            }
        }

        val getUrl = "/api/friend/1/outgoing"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(0) }
            }
        }
    }

    @Test
    fun testRevokeFriendRequestUnsuccessful() {
        val url = "/api/friend/1/revoke/100"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        post.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

        val postUrl = "/api/friend/100/revoke/1"
        val post1 = mockMvc.post(postUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        post1.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }
    }

    @Test
    fun testDeleteFriendSuccessful() {
        val url = "/api/friend/1/delete/2"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status {
                isOk()
            }
        }

        val getUrl = "/api/friend/1"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isOk()
                jsonPath("$.size()") { value(0) }
            }
        }
    }

    @Test
    fun testDeleteFriendUnsuccessful() {
        val url = "/api/friend/1/delete/100"
        val delete = mockMvc.delete(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        delete.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

        val getUrl = "/api/friend/100"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status {
                isNotFound()
            }
        }

    }
}
