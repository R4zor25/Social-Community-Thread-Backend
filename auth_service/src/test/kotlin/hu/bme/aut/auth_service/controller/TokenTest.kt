package hu.bme.aut.auth_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.auth_service.domain.AuthRequest
import hu.bme.aut.auth_service.domain.JwtResponse
import hu.bme.aut.auth_service.domain.RefreshTokenRequest
import hu.bme.aut.auth_service.domain.UserRequest
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TokenTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    @Value("\${jwt.secret}") val secret: String
) {

    @Test
    fun accessTokenContainsUserIdAndExpiresAfterOneHour() {
        val response = registerAndLogin()
        val claims = parse(response.accessToken)

        assertEquals("test", claims.subject)
        assertEquals(response.user.userId, (claims["userId"] as Number).toLong())
        assertEquals(60 * 60 * 1000L, claims.expiration.time - claims.issuedAt.time)
    }

    @Test
    fun refreshedAccessTokenContainsUserId() {
        val login = registerAndLogin()

        val result = mockMvc.post("/api/auth/refreshToken") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RefreshTokenRequest(login.token))
        }.andExpect { status { isOk() } }.andReturn()

        val refreshed = objectMapper.readValue(result.response.contentAsString, JwtResponse::class.java)
        assertEquals(login.user.userId, (parse(refreshed.accessToken)["userId"] as Number).toLong())
    }

    @Test
    fun unknownRefreshTokenIsUnauthorized() {
        mockMvc.post("/api/auth/refreshToken") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(RefreshTokenRequest("unknown-token"))
        }.andExpect { status { isUnauthorized() } }
    }

    private fun registerAndLogin(): JwtResponse {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UserRequest("test", "test", "test"))
        }.andExpect { status { isOk() } }

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest("test", "test"))
        }.andExpect { status { isOk() } }.andReturn()
        val response = objectMapper.readValue(result.response.contentAsString, JwtResponse::class.java)
        assertTrue(response.accessToken.isNotEmpty())
        return response
    }

    private fun parse(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
            .build()
            .parseClaimsJws(token)
            .body
}
