package hu.bme.aut.thread

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * Real PostgreSQL and Kafka, one shared Spring context; tables are emptied before each test.
 * Requests authenticate with jwt(), so the (unreachable) key set is never fetched.
 */
@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/.well-known/jwks.json",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-service",
        "spring.security.oauth2.resourceserver.jwt.audiences=social-community"
    ]
)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jdbc: JdbcTemplate

    @BeforeEach
    fun cleanDatabase() {
        jdbc.execute(
            "TRUNCATE user_projection, threads, thread_images, thread_followers, posts, post_attachments, " +
                "post_votes, saved_posts, comments, comment_votes RESTART IDENTITY CASCADE"
        )
    }

    fun asUser(id: Long, username: String) = jwt().jwt { it.subject(id.toString()).claim("preferred_username", username) }

    val alice = asUser(1, "alice")
    val bob = asUser(2, "bob")

    fun createThread(as_: org.springframework.test.web.servlet.request.RequestPostProcessor = alice, name: String = "Kotlin", description: String = "All about Kotlin"): Long =
        idFrom(mockMvc.post("/api/v2/threads") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name","description":"$description"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString)

    fun createPost(threadId: Long, as_: org.springframework.test.web.servlet.request.RequestPostProcessor = alice, title: String = "Hello", body: String = "First post"): Long =
        idFrom(mockMvc.post("/api/v2/threads/$threadId/posts") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"$title","body":"$body","tags":["intro"]}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString)

    fun createComment(postId: Long, as_: org.springframework.test.web.servlet.request.RequestPostProcessor = alice, body: String = "Nice"): Long =
        idFrom(mockMvc.post("/api/v2/posts/$postId/comments") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"body":"$body"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString)

    fun count(table: String): Long = jdbc.queryForObject("select count(*) from $table", Long::class.java)!!

    private fun idFrom(json: String) = Regex("\"id\":(\\d+)").find(json)!!.groupValues[1].toLong()
}
