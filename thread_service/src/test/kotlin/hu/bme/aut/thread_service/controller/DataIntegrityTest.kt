package hu.bme.aut.thread_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.thread_service.models.entities.AppUser
import hu.bme.aut.thread_service.models.entities.CommentModel
import hu.bme.aut.thread_service.models.entities.ThreadPost
import hu.bme.aut.thread_service.models.entities.TopicThread
import hu.bme.aut.thread_service.repositories.CommentRepository
import hu.bme.aut.thread_service.repositories.PostRepository
import hu.bme.aut.thread_service.repositories.ThreadRepository
import hu.bme.aut.thread_service.repositories.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.springframework.test.web.servlet.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DataIntegrityTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val userRepository: UserRepository,
    val threadRepository: ThreadRepository,
    val postRepository: PostRepository,
    val commentRepository: CommentRepository,
    val transactionManager: PlatformTransactionManager
) {
    private var aliceId = 0L
    private var bobId = 0L
    private var threadId = 0L
    private var alicePostId = 0L
    private var bobPostId = 0L

    /** Alice and Bob post in one thread; Bob comments on, saves and upvotes Alice's post and follows the thread. */
    @BeforeEach
    fun setUp() = TransactionTemplate(transactionManager).executeWithoutResult {
        val alice = userRepository.save(AppUser(userName = "alice", email = "alice", password = "pw"))
        val bob = userRepository.save(AppUser(userName = "bob", email = "bob", password = "pw"))
        val thread = threadRepository.save(TopicThread(name = "Thread", description = "Description"))
        val alicePost = postRepository.save(ThreadPost(topicThread = thread, author = alice, title = "Alice's post"))
        val bobPost = postRepository.save(ThreadPost(topicThread = thread, author = bob, title = "Bob's post"))
        val comment = commentRepository.save(CommentModel(threadPost = alicePost, author = bob, commentText = "Nice"))
        bob.apply {
            savedPosts.add(alicePost)
            upvotedPosts.add(alicePost)
            upvotedComments.add(comment)
            followedThreads.add(thread)
        }
        userRepository.save(bob)
        aliceId = alice.userId
        bobId = bob.userId
        threadId = thread.topicThreadId!!
        alicePostId = alicePost.postId!!
        bobPostId = bobPost.postId!!
    }

    @Test
    fun deletingAPostKeepsUsersTheThreadAndOtherPosts() {
        mockMvc.delete("/api/thread/$aliceId/$threadId/$alicePostId/delete") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isOk() } }

        assertFalse(postRepository.existsById(alicePostId))
        assertEquals(0, commentRepository.count())
        assertTrue(postRepository.existsById(bobPostId))
        assertTrue(threadRepository.existsById(threadId))
        assertEquals(setOf("alice", "bob"), userRepository.findAll().map { it.userName }.toSet())
    }

    @Test
    fun deletingAThreadRemovesItsPostsButKeepsUsers() {
        mockMvc.delete("/api/thread/$aliceId/$threadId/delete") {
            header("X-User-Id", aliceId.toString())
        }.andExpect { status { isOk() } }

        assertFalse(threadRepository.existsById(threadId))
        assertEquals(0, postRepository.count())
        assertEquals(0, commentRepository.count())
        assertEquals(setOf("alice", "bob"), userRepository.findAll().map { it.userName }.toSet())
    }

    @Test
    fun createPostIgnoresVoteCountAndTimeFromTheBody() {
        val forgedTime = Date(0)
        mockMvc.post("/api/thread/$aliceId/$threadId/create") {
            header("X-User-Id", aliceId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(ThreadPost(title = "Forged", voteNumber = 100_000, postTime = forgedTime))
        }.andExpect { status { isOk() } }

        val created = postRepository.findAll().single { it.title == "Forged" }
        assertEquals(0, created.voteNumber)
        assertTrue(created.postTime.after(forgedTime))
    }

    @Test
    fun postCommentIgnoresVoteCountAndTimeFromTheBody() {
        val forgedTime = Date(0)
        mockMvc.post("/api/thread/$aliceId/$threadId/$bobPostId/comment") {
            header("X-User-Id", aliceId.toString())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CommentModel(commentText = "Forged", voteNumber = 100_000, commentTime = forgedTime))
        }.andExpect { status { isOk() } }

        val created = commentRepository.findAll().single { it.commentText == "Forged" }
        assertEquals(0, created.voteNumber)
        assertTrue(created.commentTime.after(forgedTime))
    }
}
