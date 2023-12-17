package hu.bme.aut.auth_service.config

import hu.bme.aut.auth_service.domain.AppUser
import hu.bme.aut.auth_service.repositories.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import java.util.*


@Component
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val credential: Optional<AppUser> = userRepository.findByUserName(username)
        return credential.map { appUser: AppUser -> CustomUserDetails(appUser) }.orElseThrow { UsernameNotFoundException("user not found with name :$username") }
    }
}