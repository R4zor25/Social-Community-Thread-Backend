package hu.bme.aut.auth.api

import hu.bme.aut.auth.domain.TooManyLoginAttemptsException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(TooManyLoginAttemptsException::class)
    fun tooManyAttempts(e: TooManyLoginAttemptsException): ResponseEntity<ProblemDetail> {
        val seconds = maxOf(1, (e.retryAfter.toMillis() + 999) / 1000)
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, seconds.toString())
            .body(ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts"))
    }
}
