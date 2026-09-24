package hu.bme.aut.common.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/** Like an unreachable key set: the decoder fails for a reason other than a bad token. */
@TestConfiguration
class KeysUnavailable {
    @Bean fun jwtDecoder() = JwtDecoder { throw JwtException("Couldn't retrieve remote JWK set") }
}

@SpringBootTest(classes = [TestApplication::class, TestController::class, KeysUnavailable::class])
@AutoConfigureMockMvc
class KeyOutageTest @Autowired constructor(val mockMvc: MockMvc) {

    @Test
    fun tokensThatCannotBeVerifiedRightNowGetA503Problem() {
        mockMvc.get("/test/me") { header("Authorization", "Bearer some.jwt.token") }.andExpect {
            status { isServiceUnavailable() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(503) }
        }
    }
}
