package hu.bme.aut.thread_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import hu.bme.aut.thread_service.repositories.PostRepository
import hu.bme.aut.thread_service.repositories.ThreadRepository
import hu.bme.aut.thread_service.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthorizationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val userRepository: UserRepository,
    val threadRepository: ThreadRepository,
    val postRepository: PostRepository
) {
    private var aliceId = 0L
    private var bobId = 0L
    private var threadId = 0L
    private var otherThreadId = 0L
    private var alicePostId = 0L

    @BeforeEach
    fun setUp() {
        aliceId = userRepository.save(AppUser(userName = "alice", email = "alice", password = "pw")).userId
        bobId = userRepository.save(AppUser(userName = "bob", email = "bob", password = "pw")).userId
        val alice = userRepository.findById(aliceId).get()
        val thread = threadRepository.save(TopicThread(name = "Thread", description = "Description", creator = alice))
        threadId = thread.topicThreadId!!
        otherThreadId = threadRepository.save(TopicThread(name = "Other", description = "Other", creator = alice)).topicThreadId!!
        alicePostId = postRepository.save(ThreadPost(
            topicThread = thread,
            author = userRepository.findById(aliceId).get(),
            title = "Alice's post"
        )).postId!!
    }

    @Test
    fun missingUserIdHeaderIsUnauthorized() {
        mockMvc.get("/api/thread/$aliceId/saved").andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/thread/search?containsString=Thread").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun actingAsAnotherUserIsForbidden() {
        mockMvc.get("/api/thread/$aliceId/saved") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun onlyTheAuthorCanDeleteAPost() {
        mockMvc.delete("/api/thread/$bobId/$threadId/$alicePostId/delete") {
            header("X-User-Id", bobId.toString())
        }.andExpect { status { isForbidden() } }
        assertTrue(postRepository.existsById(alicePostId))

        mockMvc.delete("/api/thread/$aliceId/$threadId/$alicePostId/delete") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isOk() } }
    }

    @Test
    fun createPostWithAnExistingIdDoesNotOverwriteIt() {
        mockMvc.post("/api/thread/$bobId/$threadId/create") {
            header("X-User-Id", bobId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ThreadPost(postId = alicePostId, title = "Hijacked"))
        }.andExpect { status { isOk() } }

        val posts = postRepository.findAll()
        assertEquals(2, posts.size)
        val alicePost = posts.single { it.postId == alicePostId }
        assertEquals("Alice's post", alicePost.title)
        assertEquals(aliceId, alicePost.author.userId)
    }

    @Test
    fun modifyUpdatesTheThreadFromThePathNotFromTheBody() {
        mockMvc.put("/api/thread/$aliceId/$threadId/modify") {
            header("X-User-Id", aliceId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                TopicThread(topicThreadId = otherThreadId, name = "Renamed", description = "Changed")
            )
        }.andExpect { status { isOk() } }

        assertEquals("Renamed", threadRepository.findById(threadId).get().name)
        assertEquals("Other", threadRepository.findById(otherThreadId).get().name)
    }

    private fun modifyThread(userId: Long, id: Long) = mockMvc.put("/api/thread/$userId/$id/modify") {
        header("X-User-Id", userId.toString())
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(TopicThread(name = "Renamed", description = "Changed"))
    }

    private fun deleteThread(userId: Long, id: Long) = mockMvc.delete("/api/thread/$userId/$id/delete") {
        header("X-User-Id", userId.toString())
    }

    @Test
    fun onlyTheCreatorCanModifyOrDeleteAThread() {
        mockMvc.post("/api/thread/$aliceId/create") {
            header("X-User-Id", aliceId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(TopicThread(name = "Created", description = "By alice"))
        }.andExpect { status { isOk() } }
        val created = threadRepository.findAll().single { it.name == "Created" }
        assertEquals(aliceId, created.creator?.userId)
        val createdId = created.topicThreadId!!

        modifyThread(bobId, createdId).andExpect { status { isForbidden() } }
        deleteThread(bobId, createdId).andExpect { status { isForbidden() } }
        modifyThread(aliceId, createdId).andExpect { status { isOk() } }
        deleteThread(aliceId, createdId).andExpect { status { isOk() } }
    }

    @Test
    fun threadWithoutCreatorCannotBeModifiedOrDeleted() {
        val legacyId = threadRepository.save(TopicThread(name = "Legacy", description = "No creator")).topicThreadId!!

        modifyThread(aliceId, legacyId).andExpect { status { isForbidden() } }
        deleteThread(aliceId, legacyId).andExpect { status { isForbidden() } }
        assertTrue(threadRepository.existsById(legacyId))
    }

    @Test
    fun responsesDoNotContainOtherUsersEmail() {
        mockMvc.get("/api/thread/$bobId/$threadId/posts") {
            header("X-User-Id", bobId.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].author.userName") { value("alice") }
            jsonPath("$[0].author.email") { doesNotExist() }
        }
    }
}
