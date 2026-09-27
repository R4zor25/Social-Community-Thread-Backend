package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** thread-service never knows email addresses; no response may carry such a field. */
class ResponsePrivacyTest : IntegrationTest() {

    @Test
    fun noResponseContainsAnEmailField() {
        val thread = createThread()
        val post = createPost(thread, as_ = bob)
        createComment(post)

        listOf("/api/v2/threads", "/api/v2/threads/$thread", "/api/v2/threads/$thread/posts", "/api/v2/posts/$post",
            "/api/v2/posts/$post/comments", "/api/v2/posts", "/api/v2/feed").forEach { path ->
            val body = mockMvc.get(path) { with(alice) }.andExpect { status { isOk() } }.andReturn().response.contentAsString
            assertThat(body).describedAs(path).doesNotContain("email")
        }
    }
}
