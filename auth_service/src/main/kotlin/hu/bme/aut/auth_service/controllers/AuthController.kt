package hu.bme.aut.auth_service.controllers


import hu.bme.aut.common.identity.PublicEndpoint
import hu.bme.aut.common.identity.USER_ID_HEADER
import hu.bme.aut.auth_service.domain.*
import hu.bme.aut.auth_service.services.JwtService
import hu.bme.aut.auth_service.services.RefreshTokenService
import hu.bme.aut.auth_service.services.UserService
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
class AuthController(
    private var userService: UserService,
    private var jwtService: JwtService,
    private var refreshTokenService: RefreshTokenService,
    private var authenticationManager: AuthenticationManager
) {
    //@PreAuthorize("hasAuthority('Admin')")
    @GetMapping("/users")
    fun findAll(@RequestHeader(USER_ID_HEADER) viewerId: Long): ResponseEntity<List<UserResponse>> =
        ResponseEntity.ok(userService.findAll().map { UserResponse.of(it, viewerId) })

    //@PreAuthorize("hasAuthority('Admin')")
    @GetMapping("/users/{id}")
    fun findById(@RequestHeader(USER_ID_HEADER) viewerId: Long, @PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse.of(user, viewerId))
    }

    //@PreAuthorize("hasAuthority('Admin') || hasAuthority('User')")
    @GetMapping("/users/username/{username}")
    fun findByUsername(@RequestHeader(USER_ID_HEADER) viewerId: Long, @PathVariable username: String): ResponseEntity<Any> {
        val user = userService.findByUsername(username)  ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse.of(user, viewerId))
    }

    @PutMapping("/users/{id}/update")
    fun update(@RequestHeader(USER_ID_HEADER) actingUserId: Long, @PathVariable id: Long, @RequestBody appUser: AppUser): ResponseEntity<Any> {
        if (id != actingUserId) return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        return ResponseEntity.ok().body(userService.update(id, appUser))
    }


    @PublicEndpoint
    @PostMapping("/register")
    fun register(@RequestBody userRequest: UserRequest): ResponseEntity<Any> = userService.create(userRequest)

    @PublicEndpoint
    @PostMapping("/login")
    fun login(@RequestBody authRequest: AuthRequest): ResponseEntity<Any> {
        val authenticate: Authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken(authRequest.username, authRequest.password))
        return if (authenticate.isAuthenticated) {
            val appUser = userService.findByUsername(authRequest.username)!!
            val refreshToken = refreshTokenService.createRefreshToken(authRequest.username)
            val accessToken = jwtService.generateToken(appUser.userId, appUser.userName)
            ResponseEntity.ok().body(JwtResponse(accessToken, refreshToken.token, appUser))
        } else {
            return ResponseEntity.status(401).body("Login failed!")
        }
    }

    @PublicEndpoint
    @PostMapping("/refreshToken")
    fun refreshToken(@RequestBody refreshTokenRequest: RefreshTokenRequest): ResponseEntity<JwtResponse> {
        val refreshToken = refreshTokenService.findByToken(refreshTokenRequest.token).orElse(null)
            ?.let { refreshTokenService.verifyExpiration(it) }
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val user = refreshToken.user
        return ResponseEntity.ok(JwtResponse().apply {
            this.accessToken = jwtService.generateToken(user.userId, user.userName)
            this.token = refreshTokenRequest.token
            this.user = user
        })
    }

    @PublicEndpoint
    @PostMapping("/validate")
    fun validateToken(@RequestBody token: String): String {
        jwtService.validateToken(token)
        return "Token is valid"
    }
}