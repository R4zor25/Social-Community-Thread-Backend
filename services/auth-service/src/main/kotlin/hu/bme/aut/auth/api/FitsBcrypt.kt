package hu.bme.aut.auth.api

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/** BCrypt uses at most 72 bytes of a password, so a longer one is rejected instead of being cut off or failing. */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FitsBcryptValidator::class])
annotation class FitsBcrypt(
    val message: String = "must be at most 72 bytes in UTF-8",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class FitsBcryptValidator : ConstraintValidator<FitsBcrypt, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext) = value == null || value.toByteArray().size <= 72
}
