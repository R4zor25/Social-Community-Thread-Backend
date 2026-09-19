package hu.bme.aut.common.web.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler

/** Keeps the standard WWW-Authenticate headers and adds a problem+json body. */
class ProblemAuthenticationEntryPoint : AuthenticationEntryPoint {
    private val bearer = BearerTokenAuthenticationEntryPoint()
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, e: AuthenticationException) {
        bearer.commence(request, response, e)
        writeProblem(response, HttpStatus.UNAUTHORIZED, "A valid access token is required", request.requestURI)
    }
}

class ProblemAccessDeniedHandler : AccessDeniedHandler {
    private val bearer = BearerTokenAccessDeniedHandler()
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, e: AccessDeniedException) {
        bearer.handle(request, response, e)
        writeProblem(response, HttpStatus.FORBIDDEN, "Access denied", request.requestURI)
    }
}

private fun writeProblem(response: HttpServletResponse, status: HttpStatus, detail: String, instance: String) {
    response.status = status.value()
    response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
    response.writer.write(
        """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},"detail":"$detail","instance":"$instance"}"""
    )
}
