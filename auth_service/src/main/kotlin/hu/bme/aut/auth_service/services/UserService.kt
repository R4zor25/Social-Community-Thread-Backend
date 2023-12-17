package hu.bme.aut.auth_service.services


import hu.bme.aut.auth_service.domain.AppUser
import hu.bme.aut.auth_service.domain.UserRequest
import org.springframework.http.ResponseEntity

interface UserService {
    fun findAll(): List<AppUser>
    fun findByUsername(username: String): AppUser?
    fun findById(id: Long): AppUser?
    //fun assignToGroup(userId: Long, groupId: String)
   // fun assignRole(userId: Long, roleRepresentation: RoleRepresentation)
   // fun unassignRole(userId: Long, roleRepresentation: RoleRepresentation)
    fun create(request: UserRequest): ResponseEntity<Any>
    fun generateToken(username: String) : String
    fun validateToken(token: String)
    fun update(id: Long, appUser: AppUser) : AppUser
}