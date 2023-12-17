package hu.bme.aut.auth_service.config

import hu.bme.aut.auth_service.domain.AppUser
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


class CustomUserDetails(appUser: AppUser) : UserDetails {
    private val username: String
    private val password: String

    init {
        username = appUser.userName
        password = appUser.password
    }

    override fun getAuthorities(): Collection<GrantedAuthority?> {
        return emptyList()
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}