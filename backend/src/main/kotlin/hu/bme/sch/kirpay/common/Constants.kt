package hu.bme.sch.kirpay.common

import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest

const val ADMIN_API = "/v1/api/admin"
const val TERMINAL_API = "/v1/api/terminal"
const val APP_ENDPOINT = "/v1/api/app"

const val DEFAULT_PAGE = 0
const val DEFAULT_PAGE_SIZE = 50
const val MAX_PAGE_SIZE = 500

fun requireValidPagination(page: Int, size: Int) {
  if (page < 0) throw BadRequestException("Az oldalszám nem lehet negatív!")
  if (size < 1) throw BadRequestException("A lapméret legalább 1 kell, hogy legyen!")
  if (size > MAX_PAGE_SIZE) throw BadRequestException("A lapméret legfeljebb $MAX_PAGE_SIZE lehet!")
}

fun sha256Hex(payload: String): String =
  MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }

// A dedicated mapper keeps the fingerprint encoding stable
private val FINGERPRINT_MAPPER = ObjectMapper()

fun buildFingerprint(operationType: String, vararg parts: Any?): String =
  sha256Hex(FINGERPRINT_MAPPER.writeValueAsString(listOf(operationType, *parts)))
