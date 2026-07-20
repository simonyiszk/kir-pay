package hu.bme.sch.kirpay

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.order.*
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalDto
import hu.bme.sch.kirpay.principal.Role
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder


fun testAccount(
  id: Int? = null,
  name: String = "Test Account",
  email: String? = "test@example.com",
  phone: String? = "+36123456789",
  card: String? = "CARD-001",
  balance: Long = 1000,
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
  lastUsed: Long = 1700000000000L
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
  lastUsed = lastUsed
)

fun testItem(
  id: Int? = null,
  name: String = "Test Item",
  alias: String? = "TI",
  cost: Long = 100,
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
  idempotencyKey: String? = null
) = Order(
  id = id,
  accountId = accountId,
  timestamp = timestamp,
  idempotencyKey = idempotencyKey
)

fun testOrderLine(
  id: Int? = null,
  orderId: Int? = 1,
  itemId: Int? = 1,
  itemCount: Int = 2,
  message: String? = null,
  usedVoucher: Boolean = false,
  paidAmount: Long = 200
) = OrderLine(
  id = id,
  orderId = orderId,
  itemId = itemId,
  itemCount = itemCount,
  message = message,
  usedVoucher = usedVoucher,
  paidAmount = paidAmount
)

fun testVoucher(
  id: Int? = null,
  accountId: Int? = 1,
  itemId: Int = 1,
  count: Int = 5
) = Voucher(
  id = id,
  accountId = accountId,
  itemId = itemId,
  count = count
)

fun testOrderLineDto(
  itemId: Int? = 1,
  itemCount: Int = 2,
  usedVoucher: Boolean = false,
  message: String? = null,
  paidAmount: Long? = null
) = OrderTerminalController.OrderLineDto(
  itemId = itemId,
  itemCount = itemCount,
  usedVoucher = usedVoucher,
  message = message,
  paidAmount = paidAmount
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
