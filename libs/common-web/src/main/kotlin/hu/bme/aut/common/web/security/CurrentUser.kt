package hu.bme.aut.common.web.security

import org.springframework.core.MethodParameter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/** The authenticated caller, taken from the validated access token. */
data class CurrentUser(val id: Long, val username: String)

fun Jwt.toCurrentUser() = CurrentUser(subject.toLong(), getClaimAsString("preferred_username"))

class CurrentUserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter) = parameter.parameterType == CurrentUser::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): CurrentUser {
        val jwt = SecurityContextHolder.getContext().authentication?.principal as? Jwt
            ?: throw IllegalStateException("CurrentUser requested on an unauthenticated endpoint")
        return jwt.toCurrentUser()
    }
}
