package hu.bme.aut.chat_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.chat_service.domain.AppUser
import hu.bme.aut.chat_service.domain.ChatConversation
import hu.bme.aut.chat_service.domain.ChatMessage
import hu.bme.aut.chat_service.repositories.UserRepository
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
import java.util.*


@SpringBootTest
@AutoConfigureMockMvc
//@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ChatControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
) {

    @TestConfiguration
    internal class DatabasePreparation @Autowired constructor(
        val userRepository: UserRepository
    ) {
        @EventListener(ApplicationStartedEvent::class)
        fun preparedDbForTheTest() {
            userRepository.deleteAll()
            val user1 = AppUser(userName = "test1", email = "test1", password = "test")
            val user2 = AppUser(userName = "test2", email = "test2", password = "test")
            val user3 = AppUser(userName = "test3", email = "test3", password = "test")
            userRepository.save(user1)
            userRepository.save(user2)
            userRepository.save(user3)
        }
    }

    @Test
    fun testCreateChatConversationSuccessful() {
        val url = "/api/chat/1/create"
        val post = mockMvc.post(url) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val getUrl = "/api/chat/1/1"

        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.conversationName") { value("testConversation") }
                }
            }

    }


    @Test
    fun testCreateChatConversationUnsuccessful() {
        val invalidUrl = "/api/chat/9/create"
        val postCreatorNotFound = mockMvc.post(invalidUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        postCreatorNotFound
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }
    }

    @Test
    fun testGetAllConversations() {

        val createUrl = "/api/chat/1/create"
        val post = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val url = "/api/chat/1/conversations"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.size()") { value(1) }
                    jsonPath("$[0].conversationName") { value("testConversation") }
                }
            }
    }


    @Test
    fun testGetAllConversationsUnsuccessful() {
        val url = "/api/chat/9/conversations"
        val get = mockMvc.get(url) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isNotFound() }
        }
    }


    @Test
    fun testGetConversationSuccessful() {

        val createUrl = "/api/chat/1/create"
        val post = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }


        val getUrl = "/api/chat/1/1"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                    jsonPath("$.conversationName") { value("testConversation") }
                }
            }
    }

    @Test
    fun testGetConversationUnsuccessful() {

        val createUrl = "/api/chat/1/create"
        val post = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val userDoesNotExistgetUrl = "/api/chat/9/conversations/1"
        val conversationDoesNotExistgetUrl = "/api/chat/1/conversations/9"


        val getUserNotFound = mockMvc.get(userDoesNotExistgetUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        getUserNotFound
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val getConversationNotFound = mockMvc.get(conversationDoesNotExistgetUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        getConversationNotFound
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }
    }

    @Test
    fun testSendMessageSuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date()
                )
            )
        }

        createConversation
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val messageUrl = "/api/chat/1/1/send"
        val post = mockMvc.post(messageUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatMessage(
                    messageText = "test",
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val getUrl = "/api/chat/1/1"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.messageList[0].messageText") { value("test") }
            }
        }
    }

    @Test
    fun testSendMessageUnsuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        createConversation
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }
        val messageUrlUserNotFound = "/api/chat/9/1/send"

        val post = mockMvc.post(messageUrlUserNotFound) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatMessage(
                    messageText = "test",
                )
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val messageUrlConversationNotFound = "/api/chat/1/9/send"
        val post2 = mockMvc.post(messageUrlConversationNotFound) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatMessage(
                    messageText = "test",
                )
            )
        }

        post2
            .andDo { print() }
            .andExpect {
                status { isNotFound() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

    }

    @Test
    fun testAddUserToConversationSuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date()
                )
            )
        }

        createConversation
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val addUserUrl = "/api/chat/1/addParticipants"
        val post = mockMvc.post(addUserUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(3L)
            )
        }

        post
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val getUrl = "/api/chat/1/1"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.chatParticipants.size()") { value(2) }
            }
        }
    }

    @Test
    fun testAddUserToConversationUnsuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date(),
                )
            )
        }

        createConversation
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                }
            }

        val addUserUrlConversationDoesNotExist = "/api/chat/2/addParticipants"
        val post = mockMvc.post(addUserUrlConversationDoesNotExist) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(3L)
            )
        }

        post.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val addUserUrlUserDoesNotExist = "/api/chat/1/addParticipants"
        val post2 = mockMvc.post(addUserUrlUserDoesNotExist) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(9L)
            )
        }

        post2.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }

    @Test
    fun testRemoveUserFromConversationSuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date()
                )
            )
        }

        createConversation
            .andDo { print() }
            .andExpect {
                status { isOk() }
                content {
                    MediaType.APPLICATION_JSON
                }
            }

        val removeUserUrl = "/api/chat/1/removeParticipants"
        val delete = mockMvc.delete(removeUserUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(3L)
            )
        }

        delete.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val getUrl = "/api/chat/1/1"
        val get = mockMvc.get(getUrl) {
            contentType = MediaType.APPLICATION_JSON
        }

        get.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
                jsonPath("$.chatParticipants.size()") { value(1) }
            }
        }
    }

    @Test
    fun testRemoveUserFromConversationUnsuccessful() {
        val createUrl = "/api/chat/1/create"
        val createConversation = mockMvc.post(createUrl) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                ChatConversation(
                    conversationName = "testConversation",
                    creationDate = Date()
                )
            )
        }

        createConversation.andDo { print() }.andExpect {
            status { isOk() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val removeUserUrlConversationNotFound = "/api/chat/2/removeParticipants"
        val delete = mockMvc.delete(removeUserUrlConversationNotFound) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(3L)
            )
        }

        delete.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }

        val removeUserUrlUserNotFound = "/api/chat/1/removeParticipants"
        val delete2 = mockMvc.delete(removeUserUrlUserNotFound) {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                listOf(9L)
            )
        }

        delete2.andDo { print() }.andExpect {
            status { isNotFound() }
            content {
                MediaType.APPLICATION_JSON
            }
        }
    }
}