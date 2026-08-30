package hu.bme.sch.kirpay.common

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

abstract class KirPayException(statusCode: HttpStatus, reason: String?) : ResponseStatusException(statusCode, reason) {
  override val message: String
    get() = reason ?: statusCode.toString()
}

class NotFoundException(reason: String?) : KirPayException(HttpStatus.NOT_FOUND, reason)

class BadRequestException(reason: String?) : KirPayException(HttpStatus.BAD_REQUEST, reason)

class InternalErrorException(reason: String?) : KirPayException(HttpStatus.INTERNAL_SERVER_ERROR, reason)

class UnprocessableEntityException(reason: String?) : KirPayException(HttpStatus.UNPROCESSABLE_CONTENT, reason)
