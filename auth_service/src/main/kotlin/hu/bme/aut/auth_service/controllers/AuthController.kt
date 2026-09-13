package hu.bme.aut.auth_service.controllers


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
    fun findAll(): ResponseEntity<List<AppUser>> = ResponseEntity.ok(userService.findAll())

    //@PreAuthorize("hasAuthority('Admin')")
    @GetMapping("/users/{id}")
    fun findById(@PathVariable id: Long): ResponseEntity<AppUser> {
        val user = userService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    //@PreAuthorize("hasAuthority('Admin') || hasAuthority('User')")
    @GetMapping("/users/username/{username}")
    fun findByUsername(@PathVariable username: String): ResponseEntity<Any> {
        val user = userService.findByUsername(username)  ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @PutMapping("/users/{id}/update")
    fun update(@PathVariable id: Long, @RequestBody appUser: AppUser): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok().body(userService.update(id, appUser))
        } catch (e: Exception) {
             ResponseEntity.status(404).body(e.localizedMessage)
        }
    }


    @PostMapping("/register")
    fun register(@RequestBody userRequest: UserRequest): ResponseEntity<Any> = userService.create(userRequest)

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

    @PostMapping("/validate")
    fun validateToken(@RequestBody token: String): String {
        jwtService.validateToken(token)
        return "Token is valid"
    }
}