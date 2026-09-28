package hu.bme.aut.friend

import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.projection.UserProjections
import hu.bme.aut.testsupport.ServiceContainers
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor

/** Real PostgreSQL and Kafka, one shared context; alice, bob and carol exist in the projection before each test. */
@SpringBootTest(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:1/.well-known/jwks.json",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-service",
        "spring.security.oauth2.resourceserver.jwt.audiences=social-community"
    ]
)
@AutoConfigureMockMvc
@Import(ServiceContainers::class)
abstract class IntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var projections: UserProjections

    val alice = user(1, "alice")
    val bob = user(2, "bob")
    val carol = user(3, "carol")

    @BeforeEach
    fun resetState() {
        jdbc.execute("TRUNCATE user_projection, friend_requests, friendships RESTART IDENTITY CASCADE")
        listOf(1L to "alice", 2L to "bob", 3L to "carol").forEach { (id, name) -> projections.ensure(CurrentUser(id, name)) }
    }

    fun user(id: Long, username: String): RequestPostProcessor =
        jwt().jwt { it.subject(id.toString()).claim("preferred_username", username) }

    fun sendRequest(from: RequestPostProcessor, recipientId: Long): ResultActionsDsl = mockMvc.post("/api/v2/friend-requests") {
        with(from)
        contentType = MediaType.APPLICATION_JSON
        content = """{"recipientId":$recipientId}"""
    }

    fun requestId(): Long = jdbc.queryForObject("select id from friend_requests", Long::class.java)!!

    fun count(table: String): Long = jdbc.queryForObject("select count(*) from $table", Long::class.java)!!
}
