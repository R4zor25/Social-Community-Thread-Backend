package hu.bme.aut.common.web.error

import org.springframework.http.HttpStatus

open class ApiException(val status: HttpStatus, message: String) : RuntimeException(message)
class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)
class ForbiddenException(message: String) : ApiException(HttpStatus.FORBIDDEN, message)
class ConflictException(message: String) : ApiException(HttpStatus.CONFLICT, message)
