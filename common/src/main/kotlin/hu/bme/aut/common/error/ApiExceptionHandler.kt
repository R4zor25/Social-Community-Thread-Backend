package hu.bme.aut.common.error

import jakarta.persistence.EntityNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Maps the exceptions the services throw to status codes, with the message as the body. */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class, NoSuchElementException::class)
    fun notFound(e: RuntimeException): ResponseEntity<String> = respond(HttpStatus.NOT_FOUND, e)

    @ExceptionHandler(ForbiddenException::class)
    fun forbidden(e: ForbiddenException): ResponseEntity<String> = respond(HttpStatus.FORBIDDEN, e)

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(e: IllegalArgumentException): ResponseEntity<String> = respond(HttpStatus.BAD_REQUEST, e)

    private fun respond(status: HttpStatus, e: RuntimeException) =
        ResponseEntity.status(status).body(e.localizedMessage)
}
