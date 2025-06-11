package hu.bme.sch.kirpay.order

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.modulith.ApplicationModule


@ApplicationModule
@Table("orders")
data class Order(
  @Id var id: Int?,
  val accountId: Int,
  val timestamp: Long
)


@Table("order_lines")
data class OrderLine(
  @Id var id: Int?,
  val orderId: Int?,
  val itemId: Int?,
  val itemCount: Int,
  val message: String?,
  val usedVoucher: Boolean,
  val paidAmount: Long
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
  val paidAmount: Long
)


@Table("items")
data class Item(
  @Id var id: Int?,
  val name: String,
  val alias: String?,
  val cost: Long,
  val stock: Int,
  val enabled: Boolean
)


@Table("vouchers")
data class Voucher(
  @Id var id: Int?,
  val accountId: Int?,
  val itemId: Int,
  val count: Int
)
