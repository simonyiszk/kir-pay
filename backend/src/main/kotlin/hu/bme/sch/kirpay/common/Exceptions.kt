package hu.bme.sch.kirpay.common

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException


class NotFoundException(reason: String?) : ResponseStatusException(HttpStatus.NOT_FOUND, reason) {
  override val message: String
    get() = reason ?: statusCode.toString()
}


class BadRequestException(reason: String?) : ResponseStatusException(HttpStatus.BAD_REQUEST, reason) {
  override val message: String
    get() = reason ?: statusCode.toString()
}


class ForbiddenException(reason: String?) : ResponseStatusException(HttpStatus.FORBIDDEN, reason) {
  override val message: String
    get() = reason ?: statusCode.toString()
}


class InternalErrorException(reason: String?) : ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason) {
  override val message: String
    get() = reason ?: statusCode.toString()
}
