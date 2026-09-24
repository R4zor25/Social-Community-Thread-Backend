package hu.bme.aut.auth.domain

import hu.bme.aut.auth.persistence.AvatarRepository
import hu.bme.aut.auth.persistence.UserRepository
import hu.bme.aut.common.web.error.ApiException
import hu.bme.aut.common.web.error.NotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserProfileService(private val users: UserRepository, private val avatars: AvatarRepository) {

    @Transactional(readOnly = true)
    fun find(id: Long): User = users.findByIdOrNull(id) ?: throw NotFoundException("User does not exist")

    @Transactional(readOnly = true)
    fun search(usernamePrefix: String, pageable: Pageable): Page<User> =
        users.findByUsernameStartingWithIgnoreCaseOrderByUsernameAsc(usernamePrefix, pageable)

    @Transactional
    fun setAvatar(userId: Long, content: ByteArray, rawContentType: String) {
        if (content.size > MAX_IMAGE_BYTES) throw ApiException(HttpStatus.CONTENT_TOO_LARGE, "Images are limited to 5 MB")
        // Only type/subtype is stored, so parameters a client sends (e.g. charset) never reach the response.
        val contentType = MediaType.parseMediaType(rawContentType).let { "${it.type}/${it.subtype}" }
        find(userId)
        val avatar = avatars.findByIdOrNull(userId)
        if (avatar == null) {
            avatars.save(Avatar(userId, content, contentType))
        } else {
            avatar.content = content
            avatar.contentType = contentType
        }
    }

    @Transactional(readOnly = true)
    fun avatar(userId: Long): Avatar = avatars.findByIdOrNull(userId) ?: throw NotFoundException("User has no avatar")

    companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
    }
}
