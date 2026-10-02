package hu.bme.aut.auth.api

import hu.bme.aut.auth.domain.RegistrationService
import hu.bme.aut.auth.domain.SessionService
import hu.bme.aut.auth.domain.Tokens
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v2/auth")
class AuthController(private val registration: RegistrationService, private val sessions: SessionService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> {
        val user = registration.register(request.username, request.email, request.password)
        val id = requireNotNull(user.id)
        return ResponseEntity.created(URI("/api/v2/users/$id")).body(UserResponse(id, user.username))
    }

    /** The client address comes from X-Forwarded-For, which the gateway sets to the address it was connected from. */
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest, servletRequest: HttpServletRequest): TokenResponse =
        sessions.login(request.username, request.password, servletRequest.remoteAddr).toResponse()

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenResponse = sessions.refresh(request.refreshToken).toResponse()

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshRequest) = sessions.logout(request.refreshToken)

    private fun Tokens.toResponse() = TokenResponse(accessToken.value, refreshToken, "Bearer", accessToken.expiresIn.seconds)
}
