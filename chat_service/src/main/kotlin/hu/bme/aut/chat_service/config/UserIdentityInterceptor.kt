package hu.bme.aut.chat_service.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

const val USER_ID_HEADER = "X-User-Id"

/**
 * The gateway authenticates the caller and sets [USER_ID_HEADER]; this service trusts it.
 * Rejects requests without the header (401) and requests whose `userId` path variable
 * names a different user (403).
 */
class UserIdentityInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
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

@Configuration
class UserIdentityConfig : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(UserIdentityInterceptor())
            .addPathPatterns("/api/**")
    }
}
