package hu.bme.sch.kirpay.common

import org.postgresql.util.PSQLException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.dao.TransientDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
  @ExceptionHandler(AccessDeniedException::class, AuthorizationDeniedException::class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  fun handleAccessDeniedException(ex: RuntimeException): Map<String, String> =
    mapOf("error" to "Forbidden", "message" to (ex.message ?: "Access denied"))

  @ExceptionHandler(BadCredentialsException::class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  fun handleBadCredentialsException(ex: BadCredentialsException): Map<String, String> =
    mapOf("error" to "Unauthorized", "message" to "Hibás felhasználónév vagy jelszó!")

  @ExceptionHandler(OptimisticLockingFailureException::class)
  @ResponseStatus(HttpStatus.CONFLICT)
  fun handleOptimisticLockingFailureException(ex: OptimisticLockingFailureException): Map<String, String> =
    mapOf("error" to "Conflict", "message" to (ex.message ?: "Konfiktusos művelet, próbáld újra!"))

  @ExceptionHandler(TransientDataAccessException::class)
  @ResponseStatus(HttpStatus.CONFLICT)
  fun handleTransientDataAccessException(ex: TransientDataAccessException): Map<String, String> =
    mapOf("error" to "Conflict", "message" to (ex.message ?: "Konfiktusos művelet, próbáld újra!"))

  @ExceptionHandler(DataIntegrityViolationException::class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  fun handleDataIntegrityViolationException(ex: DataIntegrityViolationException): Map<String, String> {
    val constraintName = extractConstraintName(ex)
    val message = constraintNameToMessage(constraintName)
    return mapOf("error" to "Bad Request", "message" to message)
  }

  private fun extractConstraintName(ex: DataIntegrityViolationException): String? {
    var current: Throwable? = ex
    while (current != null) {
      if (current is PSQLException) {
        return current.serverErrorMessage?.constraint
      }
      current = current.cause
    }
    return null
  }

  fun constraintNameToMessage(constraintName: String?): String = when {
    constraintName == null -> "Érvénytelen adat!"
    constraintName.contains("card", ignoreCase = true) -> "A kártya már használatban van!"
    constraintName.contains("email", ignoreCase = true) -> "Az email már használatban van!"
    constraintName.contains("name", ignoreCase = true) || constraintName.contains("unique",
      ignoreCase = true) -> "A név már foglalt!"

    else -> "Érvénytelen adat!"
  }
}
