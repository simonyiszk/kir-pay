package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.principal.PermissionName
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class VoucherService(
  private val itemService: ItemService,
  private val voucherRepository: VoucherRepository,
  private val orderLineService: OrderLineService,
  private val events: ApplicationEventPublisher,
  private val clock: Clock
) {

  fun findAll() = voucherRepository.findAllOrderByAccountId()


  fun findPaginated(page: Int, size: Int) =
    voucherRepository.findAllOrderByAccountIdPaginated(page.toLong() * size, size)


  fun delete(voucherId: Int) {
    val voucher = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    voucherRepository.deleteById(voucherId)
    events.publishEvent(VoucherDeletedEvent(voucher, getLoggedInPrincipal()?.toRef(), clock.millis()))
  }


  fun importVouchers(vouchers: List<OrderAdminController.VoucherDto>): List<String> {
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
    return errors
  }


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
      events.publishEvent(VoucherCreatedEvent(saved, getLoggedInPrincipal()?.toRef(), clock.millis()))
    }
  }


  fun createBatchVoucher(itemId: Int, dto: OrderAdminController.VoucherBatchDto) {
    dto.accounts.forEach {
      saveVoucher(OrderAdminController.VoucherDto(accountId = it, itemId = itemId, count = dto.count))
    }
  }


  fun updateVoucherCount(voucherId: Int, count: Int): Voucher {
    if (count < 0) throw BadRequestException("A mennyiség nem lehet negatív!")
    val voucher = voucherRepository.findById(voucherId).orElseThrow { BadRequestException("Az utalvány nem létezik!") }
    val saved = voucherRepository.save(voucher.copy(count = count))
    events.publishEvent(VoucherUpdatedEvent(saved, getLoggedInPrincipal()?.toRef(), clock.millis()))
    return saved
  }


  @Secured(PermissionName.REDEEM_VOUCHERS)
  fun processVoucherRedemptionAuthorized(order: Order, dto: OrderTerminalController.OrderLineDto) {
    validateOrderLine(dto)
    val voucher = voucherRepository.findByAccountAndItem(order.accountId, dto.itemId!!)
      ?: throw BadRequestException("A számlának nincs utalványa ehhez a termékhez!")
    if (voucher.count == 0) throw BadRequestException("A számlához tartozó utalványok már beváltásra kerültek!")

    val item = itemService.findActiveItem(itemId = dto.itemId)
    if (voucher.count < dto.itemCount) throw BadRequestException("A számlának nincs elég utalványa ehhez a termékhez!")
    itemService.removeFromStock(item.id!!, dto.itemCount)

    val newVoucher = voucherRepository.save(voucher.copy(count = voucher.count - dto.itemCount))
    events.publishEvent(VoucherUpdatedEvent(newVoucher, getLoggedInPrincipal()?.toRef(), clock.millis()))

    orderLineService.save(
      OrderLine(
        id = null,
        orderId = order.id,
        itemId = item.id,
        itemCount = dto.itemCount,
        message = dto.message,
        usedVoucher = dto.usedVoucher, // True
        paidAmount = dto.paidAmount ?: 0 // Always 0, if not, we messed up
      )
    )

    events.publishEvent(
      VoucherRedeemedEvent(
        order.id,
        order.accountId,
        item.name,
        dto.message,
        dto.itemCount,
        getLoggedInPrincipal()?.toRef(),
        clock.millis(),
      )
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
