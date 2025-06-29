package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.common.AuthenticatedAppEvent
import hu.bme.sch.kirpay.principal.Principal


data class OrderCreatedEvent(
  val orderId: Int?,
  val accountId: Int,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class ItemSoldEvent(
  val orderId: Int?,
  val accountId: Int?,
  val item: String?,
  val message: String?,
  val amount: Long,
  val count: Int,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class VoucherRedeemedEvent(
  val orderId: Int?,
  val accountId: Int?,
  val item: String,
  val message: String?,
  val count: Int,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class ItemCreatedEvent(
  val item: Item,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class ItemUpdatedEvent(
  val item: Item,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class ItemDeletedEvent(
  val item: Item,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class VoucherCreatedEvent(
  val voucher: Voucher,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class VoucherUpdatedEvent(
  val voucher: Voucher,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent


data class VoucherDeletedEvent(
  val voucher: Voucher,
  override val by: Principal?,
  override val timestamp: Long
) : AuthenticatedAppEvent
