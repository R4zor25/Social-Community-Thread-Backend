package hu.bme.aut.auth.api

import hu.bme.aut.auth.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get

/** The OpenAPI document is public and describes the service's /api/v2 endpoints. */
class OpenApiTest : IntegrationTest() {

    @Test
    fun theApiDocumentIsPublishedWithoutAToken() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.openapi") { exists() }
            jsonPath("$.paths['/api/v2/auth/register']") { exists() }
        }
    }
}
