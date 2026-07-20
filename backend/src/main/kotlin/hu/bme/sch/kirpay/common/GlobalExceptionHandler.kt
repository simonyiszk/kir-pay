package hu.bme.sch.kirpay.common

import org.springframework.http.HttpStatus
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus


@ControllerAdvice
class GlobalExceptionHandler {

  @ExceptionHandler(AuthorizationDeniedException::class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  fun handleAuthorizationDeniedException(ex: AuthorizationDeniedException): Map<String, String> =
    mapOf("error" to "Forbidden", "message" to (ex.message ?: "Access denied"))
}
