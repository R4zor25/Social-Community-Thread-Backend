package hu.bme.aut.common.identity

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

const val USER_ID_HEADER = "X-User-Id"

/** Marks a handler method that is reachable without [USER_ID_HEADER], e.g. login. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint

/**
 * The gateway authenticates the caller and sets [USER_ID_HEADER]; the services trust it.
 * Rejects requests without the header (401) and requests whose `userId` path variable
 * names a different user (403).
 */
class UserIdentityInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (handler is HandlerMethod && handler.hasMethodAnnotation(PublicEndpoint::class.java)) {
            return true
        }
        val actingUserId = request.getHeader(USER_ID_HEADER)?.toLongOrNull()
        if (actingUserId == null) {
            response.status = HttpStatus.UNAUTHORIZED.value()
            return false
        }
        @Suppress("UNCHECKED_CAST")
        val pathVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as? Map<String, String>
        val pathUserId = pathVariables?.get("userId")
        if (pathUserId != null && pathUserId.toLongOrNull() != actingUserId) {
            response.status = HttpStatus.FORBIDDEN.value()
            return false
        }
        return true
    }
}
