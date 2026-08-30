package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.common.*
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.principal.PermissionName
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.*

@Service
@Transactional
class VoucherService(
  private val itemService: ItemService,
  private val voucherRepository: VoucherRepository,
  private val orderLineService: OrderLineService,
  private val eventService: EventService,
  private val clock: Clock,
  private val idempotencyService: IdempotencyService
) {
  fun findAll() = voucherRepository.findAllOrderByAccountId()

  fun findPaginated(page: Int, size: Int) =
    voucherRepository.findAllOrderByAccountIdPaginated(page.toLong() * size, size)

  fun delete(voucherId: Int) {
    val voucher = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    voucherRepository.deleteById(voucherId)
    eventService.logVoucherDeleted(voucher, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  @RetryTransaction
  fun importVouchers(
    vouchers: List<OrderAdminController.VoucherDto>,
    idempotencyKey: UUID,
    csv: String
  ): OrderAdminController.VoucherImportResult = idempotencyService.execute(
    idempotencyKey,
    IdempotentOperationType.VOUCHER_IMPORT,
    buildFingerprint(IdempotentOperationType.VOUCHER_IMPORT, csv),
    OrderAdminController.VoucherImportResult::class
  ) {
    val errors = mutableListOf<String>()
    vouchers.forEachIndexed { index, voucher ->
      try {
        if (voucher.accountId == null) {
          errors.add("Sor ${index + 1}: hiányzó account_id")
        } else {
          saveVoucher(voucher)
        }
      } catch (e: BadRequestException) {
        errors.add("Sor ${index + 1}: ${e.message}")
      }
    }
    OrderAdminController.VoucherImportResult(
      imported = vouchers.size - errors.size,
      total = vouchers.size,
      errors = errors
    )
  }.value

  fun saveVoucher(voucher: OrderAdminController.VoucherDto) {
    val existing = voucherRepository.findByAccountAndItem(accountId = voucher.accountId!!, itemId = voucher.itemId)
    if (existing != null) {
      throw BadRequestException("Már létezik #${voucher.accountId} számlához, #${voucher.itemId} termékhez utalvány")
    } else {
      val saved = voucherRepository.save(
        Voucher(
          id = null,
          accountId = voucher.accountId,
          itemId = voucher.itemId,
          count = voucher.count
        )
      )
      eventService.logVoucherCreated(saved, getLoggedInPrincipal()?.toRef(), clock.millis())
    }
  }

  @RetryTransaction
  fun createBatchVoucher(itemId: Int, dto: OrderAdminController.VoucherBatchDto): BulkResult =
    idempotencyService.execute(
      dto.idempotencyKey,
      IdempotentOperationType.VOUCHER_BATCH_CREATE,
      buildFingerprint(IdempotentOperationType.VOUCHER_BATCH_CREATE,
        itemId, dto.count, dto.accounts.joinToString(",")),
      BulkResult::class
    ) {
      dto.accounts.forEach {
        saveVoucher(OrderAdminController.VoucherDto(accountId = it, itemId = itemId, count = dto.count))
      }
      BulkResult(dto.accounts.size)
    }.value

  fun updateVoucherCount(voucherId: Int, count: Int): Voucher {
    if (count < 0) throw BadRequestException("A mennyiség nem lehet negatív!")
    val voucher = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    val saved = voucherRepository.save(voucher.copy(count = count, version = voucher.version))
    eventService.logVoucherUpdated(saved, getLoggedInPrincipal()?.toRef(), clock.millis())
    return saved
  }

  @RetryTransaction
  fun incrementCount(voucherId: Int, delta: Int, idempotencyKey: UUID): Voucher = idempotencyService.execute(
    idempotencyKey,
    IdempotentOperationType.VOUCHER_INCREMENT,
    buildFingerprint(IdempotentOperationType.VOUCHER_INCREMENT, voucherId, delta),
    Voucher::class
  ) {
    val voucher = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    val newCount = voucher.count + delta
    if (newCount < 0) throw BadRequestException("A mennyiség nem lehet negatív!")
    val rows = voucherRepository.incrementCount(voucherId, delta, voucher.version)
    if (rows == 0) throw OptimisticLockingFailureException("Az utalvány frissítése ütközött, próbáld újra!")
    val updated = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    eventService.logVoucherUpdated(updated, getLoggedInPrincipal()?.toRef(), clock.millis())
    updated
  }.value

  @Secured(PermissionName.REDEEM_VOUCHERS)
  fun processVoucherRedemptionAuthorized(order: Order, dto: OrderTerminalController.OrderLineDto) {
    validateOrderLine(dto)
    val voucher = voucherRepository.findByAccountAndItem(order.accountId, dto.itemId!!)
      ?: throw BadRequestException("A számlának nincs utalványa ehhez a termékhez!")
    if (voucher.count == 0) throw BadRequestException("A számlához tartozó utalványok már beváltásra kerültek!")

    val item = itemService.findActiveItem(itemId = dto.itemId)
    if (voucher.count < dto.itemCount) throw BadRequestException("A számlának nincs elég utalványa ehhez a termékhez!")
    itemService.removeFromStock(item, dto.itemCount)  // pass already-loaded item — avoids redundant DB lookup

    val newVoucher = voucherRepository.save(voucher.copy(count = voucher.count - dto.itemCount))
    eventService.logVoucherUpdated(newVoucher, getLoggedInPrincipal()?.toRef(), clock.millis())

    orderLineService.save(
      OrderLine(
        id = null,
        orderId = order.id,
        itemId = item.id,
        itemCount = dto.itemCount,
        message = dto.message,
        usedVoucher = dto.usedVoucher,
        paidAmount = java.math.BigInteger.valueOf(dto.paidAmount ?: 0)
      )
    )

    eventService.logVoucherRedeemed(
      order.id,
      order.accountId,
      item.name,
      dto.message,
      dto.itemCount,
      getLoggedInPrincipal()?.toRef(),
      clock.millis(),
    )
  }

  private fun validateOrderLine(line: OrderTerminalController.OrderLineDto) {
    if (line.paidAmount != null) throw BadRequestException("Az utalványokkal vásárolt termékeknek nem lehet extra költsége!")
    if (line.itemId == null) throw BadRequestException("Imseretlen utalvány")
    if (line.itemCount <= 0) throw BadRequestException("Legalább egy egységnyit szükséges megadni a tételből!")
  }

  data class AccountWithVouchers(val account: Account, val vouchers: List<VoucherWithItemName>)

  fun getVouchersWithAccount(account: Account) =
    AccountWithVouchers(account, voucherRepository.findAllByAccountIdWithItemName(account.id!!))

}
