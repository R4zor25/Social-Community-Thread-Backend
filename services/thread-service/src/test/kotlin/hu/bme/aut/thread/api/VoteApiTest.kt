package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.RequestPostProcessor

class VoteApiTest : IntegrationTest() {

    private fun vote(postId: Long, direction: String, as_: RequestPostProcessor) =
        mockMvc.put("/api/v2/posts/$postId/vote") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"direction":"$direction"}"""
        }

    private fun expectScore(postId: Long, score: Int, myVote: String?, as_: RequestPostProcessor) =
        mockMvc.get("/api/v2/posts/$postId") { with(as_) }.andExpect {
            jsonPath("$.score") { value(score) }
            if (myVote == null) jsonPath("$.myVote") { doesNotExist() } else jsonPath("$.myVote") { value(myVote) }
        }

    @Test
    fun votingIsIdempotentAndSwitchable() {
        val post = createPost(createThread())

        vote(post, "UP", bob).andExpect { status { isNoContent() } }
        vote(post, "UP", bob).andExpect { status { isNoContent() } }
        expectScore(post, 1, "UP", bob)

        vote(post, "DOWN", bob)
        expectScore(post, -1, "DOWN", bob)
        expectScore(post, -1, null, alice)

        mockMvc.delete("/api/v2/posts/$post/vote") { with(bob) }.andExpect { status { isNoContent() } }
        mockMvc.delete("/api/v2/posts/$post/vote") { with(bob) }.andExpect { status { isNoContent() } }
        expectScore(post, 0, null, bob)
    }

    @Test
    fun anInvalidDirectionIs400AndAnUnknownPostIs404() {
        val post = createPost(createThread())
        vote(post, "SIDEWAYS", bob).andExpect { status { isBadRequest() } }
        vote(999, "UP", bob).andExpect { status { isNotFound() } }
    }
}
