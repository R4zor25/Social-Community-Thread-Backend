package hu.bme.aut.common.identity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping

class UserIdentityInterceptorTest {

    private val interceptor = UserIdentityInterceptor()

    @Suppress("unused")
    private class Handlers {
        fun protectedEndpoint() {}

        @PublicEndpoint
        fun publicEndpoint() {}
    }

    private fun handler(name: String) = HandlerMethod(Handlers(), Handlers::class.java.getMethod(name))

    private fun request(userIdHeader: String? = null, pathUserId: String? = null) = MockHttpServletRequest().apply {
        if (userIdHeader != null) addHeader(USER_ID_HEADER, userIdHeader)
        if (pathUserId != null) setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mapOf("userId" to pathUserId))
    }

    private fun check(request: MockHttpServletRequest, handlerName: String = "protectedEndpoint"): Pair<Boolean, Int> {
        val response = MockHttpServletResponse()
        val proceed = interceptor.preHandle(request, response, handler(handlerName))
        return proceed to response.status
    }

    @Test
    fun missingHeaderIsUnauthorized() {
        val (proceed, status) = check(request(pathUserId = "1"))
        assertFalse(proceed)
        assertEquals(401, status)
    }

    @Test
    fun nonNumericHeaderIsUnauthorized() {
        val (proceed, status) = check(request(userIdHeader = "abc"))
        assertFalse(proceed)
        assertEquals(401, status)
    }

    @Test
    fun pathUserIdOfAnotherUserIsForbidden() {
        val (proceed, status) = check(request(userIdHeader = "1", pathUserId = "2"))
        assertFalse(proceed)
        assertEquals(403, status)
    }

    @Test
    fun matchingPathUserIdPasses() {
        assertTrue(check(request(userIdHeader = "1", pathUserId = "1")).first)
    }

    @Test
    fun endpointWithoutUserIdPathVariableOnlyNeedsTheHeader() {
        assertTrue(check(request(userIdHeader = "1")).first)
    }

    @Test
    fun publicEndpointNeedsNoHeader() {
        assertTrue(check(request(), handlerName = "publicEndpoint").first)
    }
}
