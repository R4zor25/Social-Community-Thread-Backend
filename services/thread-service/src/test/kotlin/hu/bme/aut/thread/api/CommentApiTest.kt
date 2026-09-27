package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class CommentApiTest : IntegrationTest() {

    @Test
    fun commentsAreListedOldestFirstWithTheirAuthors() {
        val post = createPost(createThread())
        createComment(post, as_ = alice, body = "first")
        createComment(post, as_ = bob, body = "second")

        mockMvc.get("/api/v2/posts/$post/comments") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].body") { value("first") }
            jsonPath("$.items[0].author.username") { value("alice") }
            jsonPath("$.items[1].body") { value("second") }
            jsonPath("$.totalItems") { value(2) }
        }
        mockMvc.get("/api/v2/posts/$post") { with(bob) }.andExpect { jsonPath("$.commentCount") { value(2) } }
    }

    @Test
    fun emptyCommentIs400AndUnknownPostIs404() {
        val post = createPost(createThread())
        mockMvc.post("/api/v2/posts/$post/comments") {
            with(bob)
            contentType = MediaType.APPLICATION_JSON
            content = """{"body":""}"""
        }.andExpect { status { isBadRequest() } }
        mockMvc.post("/api/v2/posts/999/comments") {
            with(bob)
            contentType = MediaType.APPLICATION_JSON
            content = """{"body":"x"}"""
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun commentVotesChangeTheScoreIdempotently() {
        val comment = createComment(createPost(createThread()))

        repeat(2) {
            mockMvc.put("/api/v2/comments/$comment/vote") {
                with(bob)
                contentType = MediaType.APPLICATION_JSON
                content = """{"direction":"DOWN"}"""
            }.andExpect { status { isNoContent() } }
        }

        mockMvc.get("/api/v2/posts/${jdbc.queryForObject("select post_id from comments", Long::class.java)}/comments") { with(bob) }.andExpect {
            jsonPath("$.items[0].score") { value(-1) }
            jsonPath("$.items[0].myVote") { value("DOWN") }
        }
        mockMvc.delete("/api/v2/comments/$comment/vote") { with(bob) }.andExpect { status { isNoContent() } }
        mockMvc.get("/api/v2/posts/${jdbc.queryForObject("select post_id from comments", Long::class.java)}/comments") { with(bob) }.andExpect {
            jsonPath("$.items[0].score") { value(0) }
            jsonPath("$.items[0].myVote") { doesNotExist() }
        }
    }
}
