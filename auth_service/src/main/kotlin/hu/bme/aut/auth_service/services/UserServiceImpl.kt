@file:OptIn(ExperimentalStdlibApi::class, ExperimentalStdlibApi::class)

package hu.bme.aut.auth_service.services

import hu.bme.aut.auth_service.domain.AppUser
import hu.bme.aut.auth_service.domain.UserRequest
import hu.bme.aut.auth_service.repositories.UserRepository
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import lombok.RequiredArgsConstructor
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service
@RequiredArgsConstructor
@Transactional
open class UserServiceImpl(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder
) : UserService {

    override fun findAll(): List<AppUser> = userRepository.findAll()
    override fun findByUsername(username: String): AppUser? = userRepository.findByUserName(username).getOrNull()
    override fun findById(id: Long): AppUser? = userRepository.findById(id).getOrNull()
    override fun create(request: UserRequest): ResponseEntity<Any> {

        if (userRepository.findByUserName(request.username).getOrNull() != null) {
            return ResponseEntity.status(400).body("Username already taken!")
        }

        if (userRepository.findByEmail(request.email).getOrNull() != null) {
            return ResponseEntity.status(400).body("Email address already in use!")
        }

        val appUser = AppUser().apply {
            this.userName = request.username
            this.email = request.email
            this.password = passwordEncoder.encode(request.password)
        }
        userRepository.save(appUser)
        return ResponseEntity.ok("User added successfully!")
    }

    override fun generateToken(username: String): String {
        return jwtService.generateToken(username)
    }

    override fun validateToken(token: String) {
        jwtService.validateToken(token)
    }

    override fun update(id: Long, appUser: AppUser) : AppUser {
        val user = userRepository.findById(id).getOrNull() ?: throw EntityNotFoundException("User does not exist!")
        user.profileImage = appUser.profileImage
        return userRepository.save(user)
    }
}