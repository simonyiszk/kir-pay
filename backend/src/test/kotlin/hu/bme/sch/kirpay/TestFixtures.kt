package hu.bme.sch.kirpay

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.common.IdempotencyService
import hu.bme.sch.kirpay.common.IdempotentOperation
import hu.bme.sch.kirpay.common.IdempotentOperationRepository
import hu.bme.sch.kirpay.order.Item
import hu.bme.sch.kirpay.order.Order
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalDto
import hu.bme.sch.kirpay.principal.Role
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

fun testAccount(
  id: Int? = null,
  name: String = "Test Account",
  email: String? = "test@example.com",
  phone: String? = "+36123456789",
  card: String? = "CARD-001",
  balance: java.math.BigInteger = java.math.BigInteger.valueOf(1000),
  active: Boolean = true,
  version: Int = 0
) = Account(
  id = id,
  name = name,
  email = email,
  phone = phone,
  card = card,
  balance = balance,
  active = active,
  version = version
)

fun testIdempotencyService(clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))):
    IdempotencyService {
  val repository: IdempotentOperationRepository = mockk()
  val stored = ConcurrentHashMap<String, IdempotentOperation>()
  every { repository.findByIdempotencyKey(any()) } answers { stored[firstArg<String>()] }
  every { repository.save(any<IdempotentOperation>()) } answers {
    val operation = firstArg<IdempotentOperation>()
    stored[operation.idempotencyKey] = operation
    operation
  }
  return IdempotencyService(repository, jacksonObjectMapper(), clock)
}

fun testPrincipal(
  id: Int? = null,
  name: String = "test-terminal",
  secret: String = BCryptPasswordEncoder().encode("password")!!,
  role: Role = Role.TERMINAL,
  active: Boolean = true,
  canUpload: Boolean = true,
  canTransfer: Boolean = true,
  canSellItems: Boolean = true,
  canRedeemVouchers: Boolean = true,
  canAssignCards: Boolean = true,
  createdAt: Long = 1700000000000L,
  lastUsed: Long = 1700000000000L,
  version: Int = 0
) = Principal(
  id = id,
  name = name,
  secret = secret,
  role = role,
  active = active,
  canUpload = canUpload,
  canTransfer = canTransfer,
  canSellItems = canSellItems,
  canRedeemVouchers = canRedeemVouchers,
  canAssignCards = canAssignCards,
  createdAt = createdAt,
  lastUsed = lastUsed,
  version = version
)

fun testItem(
  id: Int? = null,
  name: String = "Test Item",
  alias: String? = "TI",
  cost: java.math.BigInteger = java.math.BigInteger.valueOf(100),
  stock: Int = 50,
  enabled: Boolean = true,
  showOnLeaderboard: Boolean = false,
  version: Int = 0
) = Item(
  id = id,
  name = name,
  alias = alias,
  cost = cost,
  stock = stock,
  enabled = enabled,
  showOnLeaderboard = showOnLeaderboard,
  version = version
)

fun testOrder(
  id: Int? = null,
  accountId: Int = 1,
  timestamp: Long = 1700000000000L,
  idempotencyKey: String? = null,
  requestFingerprint: String? = null
) = Order(
  id = id,
  accountId = accountId,
  timestamp = timestamp,
  idempotencyKey = idempotencyKey,
  requestFingerprint = requestFingerprint
)

fun testPrincipalDto(
  name: String = "new-terminal",
  password: String = "password",
  role: Role = Role.TERMINAL,
  canUpload: Boolean = true,
  canTransfer: Boolean = true,
  canSellItems: Boolean = true,
  canRedeemVouchers: Boolean = true,
  canAssignCards: Boolean = true,
  active: Boolean = true
) = PrincipalDto(
  name = name,
  password = password,
  role = role,
  canUpload = canUpload,
  canTransfer = canTransfer,
  canSellItems = canSellItems,
  canRedeemVouchers = canRedeemVouchers,
  canAssignCards = canAssignCards,
  active = active
)
