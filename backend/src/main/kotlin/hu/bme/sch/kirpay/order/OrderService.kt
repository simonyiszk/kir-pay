package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountService
import hu.bme.sch.kirpay.common.RetryTransaction
import hu.bme.sch.kirpay.common.UnprocessableEntityException
import hu.bme.sch.kirpay.common.buildFingerprint
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class OrderService(
  private val accountService: AccountService,
  private val orderRepository: OrderRepository,
  private val voucherService: VoucherService,
  private val itemService: ItemService,
  private val eventService: EventService,
  private val clock: Clock
) {
  data class CheckoutResult(val order: Order, val replayed: Boolean)

  fun findAll() = orderRepository.findAllOrderByTimestampDesc()

  fun findPaginated(page: Int, size: Int): List<Order> =
    orderRepository.findAllOrderByTimestampDescPaginated(page.toLong() * size, size)

  fun findAllOrdersWithOrderLines() = orderRepository.findAllOrderWithOrderLinesOrderByTimestampDesc()

  fun getConsumptionLeaderboard(limit: Int) =
    orderRepository.findConsumptionLeaderboard(if (limit < 0) Int.MAX_VALUE else limit)

  fun findAllOrdersWithOrderLinesPaginated(page: Int, size: Int): List<OrderWithOrderLine> =
    orderRepository.findAllOrderWithOrderLinesOrderByTimestampDescPaginated(page.toLong() * size, size)

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun checkout(card: String, dto: OrderTerminalController.CheckoutDto): CheckoutResult {
    val keyStr = dto.idempotencyKey.toString()
    orderRepository.findByIdempotencyKey(keyStr)?.let { existing ->
      val fingerprint = computeCheckoutFingerprint(card, dto)
      if (fingerprint != existing.requestFingerprint) {
        throw UnprocessableEntityException("Az idempotencia kulcs foglalt más tartalommal!")
      }
      return CheckoutResult(existing, replayed = true)
    }

    val fingerprint = computeCheckoutFingerprint(card, dto)
    val order = newOrder(card, keyStr, fingerprint)
    eventService.logOrderCreated(order.id, order.accountId, getLoggedInPrincipal()?.toRef(), clock.millis())

    for (line in dto.orderLines) {
      if (line.usedVoucher) {
        voucherService.processVoucherRedemptionAuthorized(order, line)
      } else {
        itemService.processSaleAuthorized(order, line)
      }
    }

    return CheckoutResult(order, replayed = false)
  }

  private fun computeCheckoutFingerprint(card: String, dto: OrderTerminalController.CheckoutDto): String =
    buildFingerprint("CHECKOUT", card, dto.orderLines)

  private fun newOrder(card: String, idempotencyKey: String? = null, fingerprint: String? = null): Order {
    val account = accountService.findActiveByCard(card)
    return orderRepository.save(Order(id = null,
      accountId = account.id!!,
      timestamp = clock.millis(),
      idempotencyKey = idempotencyKey,
      requestFingerprint = fingerprint))
  }

}
