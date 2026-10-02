package hu.bme.aut.auth.api

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class RegisterRequest(
    @field:NotBlank @field:Size(min = 3, max = 32) @field:Pattern(regexp = "[A-Za-z0-9_.-]+")
    val username: String,
    @field:NotBlank @field:Email @field:Size(max = 254)
    val email: String,
    @field:NotBlank @field:Size(min = 8) @field:FitsBcrypt
    val password: String
)

data class LoginRequest(@field:NotBlank val username: String, @field:NotBlank val password: String)

data class RefreshRequest(@field:NotBlank val refreshToken: String)

data class TokenResponse(val accessToken: String, val refreshToken: String, val tokenType: String, val expiresIn: Long)

/** How a user is visible to other users. */
data class UserResponse(val id: Long, val username: String)

/** The caller's own account. */
data class MeResponse(val id: Long, val username: String, val email: String, val createdAt: Instant)
