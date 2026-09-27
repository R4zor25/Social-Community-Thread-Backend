package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

class SaveAndFeedTest : IntegrationTest() {

    @Test
    fun savingIsIdempotentAndShownAsSavedByMe() {
        val post = createPost(createThread())

        repeat(2) { mockMvc.put("/api/v2/posts/$post/save") { with(bob) }.andExpect { status { isNoContent() } } }
        assertThat(count("saved_posts")).isEqualTo(1)
        mockMvc.get("/api/v2/posts/$post") { with(bob) }.andExpect { jsonPath("$.savedByMe") { value(true) } }
        mockMvc.get("/api/v2/posts/$post") { with(alice) }.andExpect { jsonPath("$.savedByMe") { value(false) } }

        repeat(2) { mockMvc.delete("/api/v2/posts/$post/save") { with(bob) }.andExpect { status { isNoContent() } } }
        assertThat(count("saved_posts")).isZero()
    }

    @Test
    fun theFeedShowsPostsOfFollowedThreadsNewestFirst() {
        val kotlin = createThread(name = "Kotlin")
        val java = createThread(name = "Java")
        createPost(kotlin, title = "old kotlin")
        createPost(java, title = "java post")
        createPost(kotlin, title = "new kotlin")
        mockMvc.put("/api/v2/threads/$kotlin/follow") { with(bob) }

        mockMvc.get("/api/v2/feed") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.items[0].title") { value("new kotlin") }
            jsonPath("$.items[1].title") { value("old kotlin") }
        }
        mockMvc.get("/api/v2/feed") { with(alice) }.andExpect { jsonPath("$.totalItems") { value(0) } }
    }
}
