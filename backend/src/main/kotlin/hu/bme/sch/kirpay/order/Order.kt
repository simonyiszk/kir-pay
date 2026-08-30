package hu.bme.sch.kirpay.order

import jakarta.persistence.*
import java.math.BigDecimal
import java.math.BigInteger

@Entity
@Table(name = "orders")
data class Order(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(name = "account_id", nullable = false)
  val accountId: Int,
  @Column(nullable = false)
  val timestamp: Long,
  val idempotencyKey: String? = null,
  val requestFingerprint: String? = null
)

@Entity
@Table(name = "order_lines")
data class OrderLine(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  val orderId: Int?,
  val itemId: Int?,
  @Column(nullable = false)
  val itemCount: Int,
  val message: String?,
  @Column(nullable = false)
  val usedVoucher: Boolean,
  @Column(nullable = false, precision = 38)
  val paidAmount: BigInteger
)

data class OrderWithOrderLine(
  val orderId: Int,
  val accountId: Int,
  val timestamp: Long,
  val orderLineId: Int,
  val itemId: Int?,
  val itemCount: Int,
  val message: String?,
  val usedVoucher: Boolean,
  val paidAmount: BigDecimal
)

@Entity
@Table(name = "items")
data class Item(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false)
  val name: String,
  val alias: String?,
  @Column(nullable = false, precision = 38)
  val cost: BigInteger,
  @Column(nullable = false)
  val stock: Int,
  @Column(nullable = false)
  val enabled: Boolean,
  @Column(nullable = false)
  val showOnLeaderboard: Boolean = false,
  @Version
  val version: Int = 0
)

@Entity
@Table(name = "vouchers")
data class Voucher(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  val accountId: Int?,
  @Column(nullable = false)
  val itemId: Int,
  @Column(nullable = false)
  val count: Int,
  @Version
  val version: Int = 0
)

data class VoucherWithItemName(
  val voucherId: Int,
  val accountId: Int?,
  val itemId: Int,
  val itemName: String,
  val count: Int
)

data class ItemConsumptionLeaderboardEntry(
  val itemId: Int,
  val itemName: String,
  val itemCount: Long
)

data class ConsumptionLeaderboardEntry(
  val accountId: Int,
  val name: String,
  val email: String?,
  val itemCount: Long
)
