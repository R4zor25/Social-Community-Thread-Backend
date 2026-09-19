package hu.bme.aut.common.web

import hu.bme.aut.common.web.error.ConflictException
import hu.bme.aut.common.web.error.ForbiddenException
import hu.bme.aut.common.web.error.NotFoundException
import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.paging.toResponse
import hu.bme.aut.common.web.security.CurrentUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class TestApplication

@RestController
class TestController {
    @GetMapping("/test/me") fun me(user: CurrentUser) = user
    @GetMapping("/test/not-found") fun notFound(): Nothing = throw NotFoundException("Thread does not exist")
    @GetMapping("/test/forbidden") fun forbidden(): Nothing = throw ForbiddenException("Not yours")
    @GetMapping("/test/conflict") fun conflict(): Nothing = throw ConflictException("Already exists")
    @GetMapping("/test/integrity") fun integrity(): Nothing = throw DataIntegrityViolationException("duplicate key")
    @GetMapping("/test/page") fun page(pageable: Pageable): PageResponse<String> =
        PageImpl(listOf("a", "b"), pageable, 42).toResponse { it.uppercase() }
    @PostMapping("/test/validated") fun validated(@Valid @RequestBody body: Body) = body

    data class Body(@field:NotBlank val name: String)
}

@TestConfiguration
class RejectAllTokens {
    @Bean fun jwtDecoder() = JwtDecoder { throw BadJwtException("invalid") }
}

@SpringBootTest(classes = [TestApplication::class, TestController::class, RejectAllTokens::class])
@AutoConfigureMockMvc
class CommonWebTest @Autowired constructor(val mockMvc: MockMvc) {

    private val alice = jwt().jwt { it.subject("42").claim("preferred_username", "alice") }

    @Test
    fun requestsWithoutATokenGetAProblem401() {
        mockMvc.get("/test/me").andExpect {
            status { isUnauthorized() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.status") { value(401) }
        }
    }

    @Test
    fun invalidTokensAreRejected() {
        mockMvc.get("/test/me") { header("Authorization", "Bearer not-a-token") }
            .andExpect { status { isUnauthorized() } }
    }

    @Test
    fun healthIsPublic() {
        mockMvc.get("/actuator/health").andExpect { status { isNotFound() } }
    }

    @Test
    fun currentUserComesFromTheToken() {
        mockMvc.get("/test/me") { with(alice) }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(42) }
            jsonPath("$.username") { value("alice") }
        }
    }

    @Test
    fun apiExceptionsBecomeProblemDetails() {
        mockMvc.get("/test/not-found") { with(alice) }.andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("Thread does not exist") }
        }
        mockMvc.get("/test/forbidden") { with(alice) }.andExpect { status { isForbidden() } }
        mockMvc.get("/test/conflict") { with(alice) }.andExpect { status { isConflict() } }
        mockMvc.get("/test/integrity") { with(alice) }.andExpect { status { isConflict() } }
    }

    @Test
    fun validationErrorsListTheFields() {
        mockMvc.post("/test/validated") {
            with(alice)
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0].field") { value("name") }
        }
    }

    @Test
    fun pagesUseTheEnvelopeAndClampTheSize() {
        mockMvc.get("/test/page") { with(alice) }.andExpect {
            jsonPath("$.items") { value(arrayListOf("A", "B")) }
            jsonPath("$.page") { value(0) }
            jsonPath("$.size") { value(20) }
            jsonPath("$.totalItems") { value(42) }
            jsonPath("$.totalPages") { value(3) }
        }
        mockMvc.get("/test/page?size=1000") { with(alice) }.andExpect { jsonPath("$.size") { value(100) } }
    }
}
