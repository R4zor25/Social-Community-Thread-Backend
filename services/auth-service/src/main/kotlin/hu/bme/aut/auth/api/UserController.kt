package hu.bme.aut.auth.api

import hu.bme.aut.auth.domain.User
import hu.bme.aut.auth.domain.UserProfileService
import hu.bme.aut.common.web.paging.PageResponse
import hu.bme.aut.common.web.paging.toResponse
import hu.bme.aut.common.web.security.CurrentUser
import hu.bme.aut.common.web.upload.Upload
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v2/users")
class UserController(private val profiles: UserProfileService) {

    @GetMapping("/me")
    fun me(caller: CurrentUser): MeResponse =
        profiles.find(caller.id).let { MeResponse(requireNotNull(it.id), it.username, it.email, it.createdAt) }

    @GetMapping("/{id}")
    fun find(@PathVariable id: Long): UserResponse = profiles.find(id).toResponse()

    @GetMapping
    fun search(@RequestParam(defaultValue = "") username: String, pageable: Pageable): PageResponse<UserResponse> =
        profiles.search(username, pageable).toResponse { it.toResponse() }

    @PutMapping("/me/avatar", consumes = ["image/*"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun setAvatar(caller: CurrentUser, upload: Upload) = profiles.setAvatar(caller.id, upload)

    @GetMapping("/{id}/avatar")
    fun avatar(@PathVariable id: Long): ResponseEntity<ByteArray> = profiles.avatar(id).let {
        ResponseEntity.ok().contentType(MediaType.parseMediaType(it.contentType)).body(it.content)
    }

    private fun User.toResponse() = UserResponse(requireNotNull(id), username)
}
