package hu.bme.aut.thread.api

import hu.bme.aut.thread.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class ThreadApiTest : IntegrationTest() {

    @Test
    fun createdThreadHasTheCallerAsCreatorAndALocation() {
        val response = mockMvc.post("/api/v2/threads") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Kotlin","description":"All about Kotlin","creatorId":2,"id":99}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("Kotlin") }
            jsonPath("$.creator.id") { value(1) }
            jsonPath("$.creator.username") { value("alice") }
            jsonPath("$.followedByMe") { value(false) }
            jsonPath("$.hasImage") { value(false) }
        }.andReturn().response

        val id = jdbc.queryForObject("select id from threads", Long::class.java)
        assertThat(id).isNotEqualTo(99)
        assertThat(response.getHeader("Location")).isEqualTo("/api/v2/threads/$id")
    }

    @Test
    fun invalidThreadsNameTheField() {
        mockMvc.post("/api/v2/threads") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"","description":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun unknownThreadIs404() {
        mockMvc.get("/api/v2/threads/999") { with(alice) }.andExpect {
            status { isNotFound() }
            jsonPath("$.detail") { value("Thread does not exist") }
        }
    }

    @Test
    fun listIsNewestFirstFilteredAndPaged() {
        createThread(name = "Kotlin")
        createThread(name = "Java")
        createThread(name = "Kotlin Coroutines")

        mockMvc.get("/api/v2/threads?query=kotlin&size=1") { with(bob) }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].name") { value("Kotlin Coroutines") }
            jsonPath("$.totalItems") { value(2) }
            jsonPath("$.size") { value(1) }
        }
    }

    @Test
    fun followedFilterShowsOnlyFollowedThreads() {
        val kotlin = createThread(name = "Kotlin")
        createThread(name = "Java")
        mockMvc.put("/api/v2/threads/$kotlin/follow") { with(bob) }.andExpect { status { isNoContent() } }

        mockMvc.get("/api/v2/threads?followed=true") { with(bob) }.andExpect {
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].name") { value("Kotlin") }
            jsonPath("$.items[0].followedByMe") { value(true) }
        }
    }

    @Test
    fun followAndUnfollowAreIdempotent() {
        val id = createThread()
        repeat(2) { mockMvc.put("/api/v2/threads/$id/follow") { with(bob) }.andExpect { status { isNoContent() } } }
        assertThat(count("thread_followers")).isEqualTo(1)

        repeat(2) { mockMvc.delete("/api/v2/threads/$id/follow") { with(bob) }.andExpect { status { isNoContent() } } }
        assertThat(count("thread_followers")).isZero()
    }

    @Test
    fun onlyTheCreatorCanChangeOrDeleteAThread() {
        val id = createThread()

        mockMvc.patch("/api/v2/threads/$id") {
            with(bob)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Hijacked"}"""
        }.andExpect { status { isForbidden() } }
        mockMvc.delete("/api/v2/threads/$id") { with(bob) }.andExpect { status { isForbidden() } }

        mockMvc.patch("/api/v2/threads/$id") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"description":"Updated"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("Kotlin") }
            jsonPath("$.description") { value("Updated") }
        }
        mockMvc.delete("/api/v2/threads/$id") { with(alice) }.andExpect { status { isNoContent() } }
        assertThat(count("threads")).isZero()
    }

    @Test
    fun deletingAThreadKeepsUsersAndOtherThreads() {
        val kotlin = createThread(name = "Kotlin")
        val java = createThread(name = "Java")
        val post = createPost(kotlin, as_ = bob)
        createComment(post, as_ = alice)

        mockMvc.delete("/api/v2/threads/$kotlin") { with(alice) }.andExpect { status { isNoContent() } }

        assertThat(count("posts")).isZero()
        assertThat(count("comments")).isZero()
        assertThat(jdbc.queryForList("select id from threads", Long::class.java)).containsExactly(java)
        assertThat(count("user_projection")).isEqualTo(2)
    }

    @Test
    fun imageRoundTripLimitsAndOwnership() {
        val id = createThread()
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 9)

        mockMvc.get("/api/v2/threads/$id/image") { with(bob) }.andExpect { status { isNotFound() } }
        mockMvc.put("/api/v2/threads/$id/image") { with(bob); contentType = MediaType.IMAGE_PNG; content = png }
            .andExpect { status { isForbidden() } }
        mockMvc.put("/api/v2/threads/$id/image") { with(alice); contentType = MediaType.TEXT_PLAIN; content = "x" }
            .andExpect { status { isUnsupportedMediaType() } }
        mockMvc.put("/api/v2/threads/$id/image") { with(alice); contentType = MediaType.IMAGE_PNG; content = ByteArray(5 * 1024 * 1024 + 1) }
            .andExpect { status { isContentTooLarge() } }
        mockMvc.put("/api/v2/threads/$id/image") { with(alice); contentType = MediaType.IMAGE_PNG; content = png }
            .andExpect { status { isNoContent() } }

        val bytes = mockMvc.get("/api/v2/threads/$id/image") { with(bob) }.andExpect { status { isOk() } }.andReturn().response.contentAsByteArray
        assertThat(bytes).isEqualTo(png)
        mockMvc.get("/api/v2/threads/$id") { with(bob) }.andExpect { jsonPath("$.hasImage") { value(true) } }
    }
}
