package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

class AuthApiTest : IntegrationTest() {

    @BeforeEach
    fun registerAlice() {
        mockMvc.post("/api/v2/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"alice","email":"alice@example.com","password":"correct horse"}"""
        }.andExpect { status { isCreated() } }
    }

    private fun login(username: String, password: String, ip: String = "203.0.113.1"): ResultActionsDsl =
        mockMvc.post("/api/v2/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            header("X-Forwarded-For", ip)
            content = """{"username":"$username","password":"$password"}"""
        }

    private fun refresh(token: String) = mockMvc.post("/api/v2/auth/refresh") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"refreshToken":"$token"}"""
    }

    private fun refreshTokenFrom(result: ResultActionsDsl) =
        Regex("\"refreshToken\":\"([^\"]+)\"").find(result.andReturn().response.contentAsString)!!.groupValues[1]

    @Test
    fun loginReturnsAnAccessAndARefreshToken() {
        login("alice", "correct horse").andExpect {
            status { isOk() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(900) }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.refreshToken") { isNotEmpty() }
        }
    }

    @Test
    fun usernameIsCaseInsensitiveAtLogin() {
        login("ALICE", "correct horse").andExpect { status { isOk() } }
    }

    @Test
    fun wrongPasswordIs401() {
        login("alice", "wrong password").andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun unknownUserIs401() {
        login("nobody", "correct horse").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun throttledLoginIs429() {
        repeat(5) { login("alice", "wrong password").andExpect { status { isUnauthorized() } } }

        val response = login("alice", "correct horse").andExpect {
            status { isTooManyRequests() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }.andReturn().response

        assertThat(response.getHeader("Retry-After")!!.toLong()).isBetween(1, 900)
    }

    @Test
    fun throttlingCountsPerIpAcrossUsernames() {
        repeat(20) { login("user$it", "wrong password", ip = "198.51.100.7").andExpect { status { isUnauthorized() } } }

        login("alice", "correct horse", ip = "198.51.100.7").andExpect { status { isTooManyRequests() } }
        login("alice", "correct horse", ip = "198.51.100.8").andExpect { status { isOk() } }
    }

    @Test
    fun refreshReturnsANewRefreshToken() {
        val first = refreshTokenFrom(login("alice", "correct horse"))

        val second = refreshTokenFrom(refresh(first).andExpect { status { isOk() } })

        assertThat(second).isNotEqualTo(first)
    }

    @Test
    fun unknownRefreshTokenIs401() {
        refresh("not-a-token").andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun logoutIs204EvenForAnUnknownToken() {
        mockMvc.post("/api/v2/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"not-a-token"}"""
        }.andExpect { status { isNoContent() } }
    }
}
