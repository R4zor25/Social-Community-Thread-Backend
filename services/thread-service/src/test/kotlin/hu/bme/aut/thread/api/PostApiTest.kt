package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class PostApiTest : IntegrationTest() {

    @Test
    fun serverOwnedFieldsInTheBodyAreIgnored() {
        val thread = createThread()

        mockMvc.post("/api/v2/threads/$thread/posts") {
            with(bob)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Hi","body":"Text","tags":["a","b"],"id":77,"score":100000,"authorId":1,"createdAt":"1970-01-01T00:00:00Z"}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.author.id") { value(2) }
            jsonPath("$.author.username") { value("bob") }
            jsonPath("$.score") { value(0) }
            jsonPath("$.tags") { value(arrayListOf("a", "b")) }
            jsonPath("$.threadId") { value(thread) }
        }

        val (id, year) = jdbc.queryForMap("select id, extract(year from created_at)::int as year from posts").let { it["id"] to it["year"] }
        assertThat(id).isNotEqualTo(77L)
        assertThat(year).isNotEqualTo(1970)
    }

    @Test
    fun postingIntoAnUnknownThreadIs404() {
        mockMvc.post("/api/v2/threads/999/posts") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"Hi","body":""}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("Thread does not exist") }
        }
    }

    @Test
    fun postsOfAThreadAreNewestFirstAndSearchable() {
        val thread = createThread()
        createPost(thread, title = "Coroutines intro")
        createPost(thread, title = "Flows")
        createPost(thread, title = "Coroutines advanced")
        createPost(createThread(name = "Other"), title = "Coroutines elsewhere")

        mockMvc.get("/api/v2/threads/$thread/posts") { with(bob) }.andExpect {
            jsonPath("$.totalItems") { value(3) }
            jsonPath("$.items[0].title") { value("Coroutines advanced") }
        }
        mockMvc.get("/api/v2/threads/$thread/posts?query=COROUTINES") { with(bob) }.andExpect {
            jsonPath("$.totalItems") { value(2) }
        }
    }

    @Test
    fun postsCanBeFilteredByAuthorSavedAndVoted() {
        val thread = createThread()
        val byAlice = createPost(thread, as_ = alice, title = "by alice")
        val byBob = createPost(thread, as_ = bob, title = "by bob")
        mockMvc.put("/api/v2/posts/$byAlice/save") { with(bob) }
        vote(byBob, "DOWN", bob)
        vote(byAlice, "UP", bob)

        fun titles(query: String) = Regex("\"title\":\"([^\"]+)\"").findAll(
            mockMvc.get("/api/v2/posts?$query") { with(bob) }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        ).map { it.groupValues[1] }.toList()

        assertThat(titles("authorId=1")).containsExactly("by alice")
        assertThat(titles("saved=true")).containsExactly("by alice")
        assertThat(titles("voted=DOWN")).containsExactly("by bob")
        assertThat(titles("authorId=2&voted=UP")).isEmpty()
        assertThat(titles("")).containsExactly("by bob", "by alice")
    }

    @Test
    fun onlyTheAuthorCanDeleteAPostAndOnlyItsContentGoes() {
        val thread = createThread()
        val post = createPost(thread, as_ = alice)
        val other = createPost(thread, as_ = bob)
        listOf(post, other).forEach {
            createComment(it, as_ = bob)
            vote(it, "UP", bob)
            mockMvc.put("/api/v2/posts/$it/save") { with(bob) }
        }

        mockMvc.delete("/api/v2/posts/$post") { with(bob) }.andExpect {
            status { isForbidden() }
            jsonPath("$.detail") { value("Only the author can delete this post") }
        }
        mockMvc.delete("/api/v2/posts/$post") { with(alice) }.andExpect { status { isNoContent() } }

        assertThat(jdbc.queryForList("select id from posts", Long::class.java)).containsExactly(other)
        listOf("comments", "post_votes", "saved_posts").forEach {
            assertThat(jdbc.queryForList("select post_id from $it", Long::class.java)).describedAs(it).containsExactly(other)
        }
        assertThat(count("threads")).isEqualTo(1)
        assertThat(count("user_projection")).isEqualTo(2)
    }

    @Test
    fun attachmentAcceptsImagesAndVideosFromTheAuthorOnly() {
        val post = createPost(createThread(), as_ = alice)

        mockMvc.put("/api/v2/posts/$post/attachment") { with(bob); contentType = MediaType.IMAGE_GIF; content = byteArrayOf(1) }
            .andExpect { status { isForbidden() } }
        mockMvc.put("/api/v2/posts/$post/attachment") { with(alice); contentType = MediaType.APPLICATION_PDF; content = byteArrayOf(1) }
            .andExpect { status { isUnsupportedMediaType() } }
        mockMvc.put("/api/v2/posts/$post/attachment") { with(alice); contentType = MediaType.parseMediaType("video/mp4"); content = byteArrayOf(1, 2, 3) }
            .andExpect { status { isNoContent() } }

        mockMvc.get("/api/v2/posts/$post") { with(bob) }.andExpect { jsonPath("$.attachmentType") { value("video/mp4") } }
        val bytes = mockMvc.get("/api/v2/posts/$post/attachment") { with(bob) }.andExpect { status { isOk() } }.andReturn().response.contentAsByteArray
        assertThat(bytes).isEqualTo(byteArrayOf(1, 2, 3))
    }

    private fun vote(postId: Long, direction: String, as_: org.springframework.test.web.servlet.request.RequestPostProcessor) {
        mockMvc.put("/api/v2/posts/$postId/vote") {
            with(as_)
            contentType = MediaType.APPLICATION_JSON
            content = """{"direction":"$direction"}"""
        }.andExpect { status { isNoContent() } }
    }
}
