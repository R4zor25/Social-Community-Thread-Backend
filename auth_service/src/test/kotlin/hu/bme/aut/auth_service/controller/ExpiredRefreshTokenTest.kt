package hu.bme.aut.auth_service.controller

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.aut.auth_service.domain.AuthRequest
import hu.bme.aut.auth_service.domain.JwtResponse
import hu.bme.aut.auth_service.domain.RefreshTokenRequest
import hu.bme.aut.auth_service.domain.UserRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest(properties = ["jwt.refresh-token-ttl=-1s"])
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpiredRefreshTokenTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {

    @Test
    fun expiredRefreshTokenIsUnauthorized() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(UserRequest("test", "test", "test"))
        }.andExpect { status { isOk() } }

        val login = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(AuthRequest("test", "test"))
        }.andExpect { status { isOk() } }.andReturn()
        val refreshToken = objectMapper.readValue(login.response.contentAsString, JwtResponse::class.java).token

        val refresh = {
            mockMvc.post("/api/auth/refreshToken") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(RefreshTokenRequest(refreshToken))
            }
        }
        refresh().andExpect { status { isUnauthorized() } }
        // The expired token is deleted on first use, so a retry is rejected as unknown.
        refresh().andExpect { status { isUnauthorized() } }
    }
}
