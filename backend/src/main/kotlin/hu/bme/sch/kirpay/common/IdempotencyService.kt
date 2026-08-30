package hu.bme.sch.kirpay.common

import org.springframework.dao.ConcurrencyFailureException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.util.*
import kotlin.reflect.KClass

object IdempotentOperationType {
  const val PAY = "PAY"
  const val UPLOAD = "UPLOAD"
  const val TRANSFER = "TRANSFER"
  const val ACCOUNT_CREATE = "ACCOUNT_CREATE"
  const val ACCOUNT_IMPORT = "ACCOUNT_IMPORT"
  const val ITEM_CREATE = "ITEM_CREATE"
  const val ITEM_IMPORT = "ITEM_IMPORT"
  const val PRINCIPAL_CREATE = "PRINCIPAL_CREATE"
  const val PRINCIPAL_IMPORT = "PRINCIPAL_IMPORT"
  const val VOUCHER_BATCH_CREATE = "VOUCHER_BATCH_CREATE"
  const val VOUCHER_INCREMENT = "VOUCHER_INCREMENT"
  const val VOUCHER_IMPORT = "VOUCHER_IMPORT"

  val ALL = setOf(
    PAY, UPLOAD, TRANSFER,
    ACCOUNT_CREATE, ACCOUNT_IMPORT,
    ITEM_CREATE, ITEM_IMPORT,
    PRINCIPAL_CREATE, PRINCIPAL_IMPORT,
    VOUCHER_BATCH_CREATE, VOUCHER_INCREMENT, VOUCHER_IMPORT
  )
}

data class BulkResult(val affected: Int)

@Service
class IdempotencyService(
  private val idempotentOperationRepository: IdempotentOperationRepository,
  private val objectMapper: ObjectMapper,
  private val clock: Clock
) {
  data class Result<T>(val value: T, val replayed: Boolean = false)

  fun <T : Any> execute(
    key: UUID,
    operationType: String,
    fingerprint: String,
    responseType: KClass<T>,
    executeBlock: () -> T
  ): Result<T> {
    if (operationType !in IdempotentOperationType.ALL) {
      throw InternalErrorException("Ismeretlen idempotens művelettípus: $operationType")
    }

    val keyStr = key.toString()
    idempotentOperationRepository.findByIdempotencyKey(keyStr)?.let { existing ->
      if (fingerprint != existing.requestFingerprint) {
        throw UnprocessableEntityException("Az idempotencia kulcs foglalt!")
      }
      return Result(objectMapper.readValue(existing.responseJson, responseType.java), replayed = true)
    }

    val value = executeBlock()
    try {
      idempotentOperationRepository.save(
        IdempotentOperation(
          id = null,
          idempotencyKey = keyStr,
          operationType = operationType,
          requestFingerprint = fingerprint,
          responseJson = objectMapper.writeValueAsString(value),
          createdAt = clock.millis()
        )
      )
    } catch (e: DataIntegrityViolationException) {
      throw ConcurrencyFailureException("Az idempotencia konfliktus került detektálásra!", e)
    }
    return Result(value, replayed = false)
  }

}
