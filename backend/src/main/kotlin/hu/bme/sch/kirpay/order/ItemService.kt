package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountBalanceService
import hu.bme.sch.kirpay.common.*
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.principal.PermissionName
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import hu.bme.sch.kirpay.transaction.TransactionService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Clock
import java.util.*

@Service
@Transactional
class ItemService(
  private val accountBalanceService: AccountBalanceService,
  private val itemRepository: ItemRepository,
  private val orderLineService: OrderLineService,
  private val transactionService: TransactionService,
  private val eventService: EventService,
  private val clock: Clock,
  private val idempotencyService: IdempotencyService
) {
  fun find(id: Int): Item = itemRepository.findById(id).orElseThrow { BadRequestException("A termék nem létezik!") }

  fun findAll() = itemRepository.findAllOrderByName()

  fun findAllActive(): List<Item> = itemRepository.findByEnabledOrderByName(true)

  fun findPaginated(page: Int, size: Int) = itemRepository.findAllOrderByNamePaginated(page.toLong() * size, size)

  @RetryTransaction
  fun createItem(dto: ItemAdminController.ItemCreateDto): Item = idempotencyService.execute(
    dto.idempotencyKey,
    IdempotentOperationType.ITEM_CREATE,
    buildFingerprint(IdempotentOperationType.ITEM_CREATE,
      dto.name, dto.alias, dto.cost, dto.stock, dto.enabled, dto.showOnLeaderboard),
    Item::class
  ) {
    val item = try {
      itemRepository.saveAndFlush(dto.toItem())
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("A termék név már használatban van!")
    }
    eventService.logItemCreated(item, getLoggedInPrincipal()?.toRef(), clock.millis())
    item
  }.value

  @RetryTransaction
  fun updateItem(id: Int, dto: Item): Item {
    if (!itemRepository.existsById(id)) throw BadRequestException("A termék nem létezik!")
    val existing = find(id)

    val item = try {
      itemRepository.saveAndFlush(dto.copy(id = id, version = existing.version))
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("A termék név már használatban van!")
    }
    eventService.logItemUpdated(item, getLoggedInPrincipal()?.toRef(), clock.millis())
    return item
  }

  fun deleteItem(itemId: Int) {
    val item = find(itemId)
    itemRepository.deleteById(itemId)
    eventService.logItemDeleted(item, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  @RetryTransaction
  fun setEnabled(itemId: Int, enabled: Boolean): Item {
    val item = itemRepository.save(find(itemId).copy(enabled = enabled))
    eventService.logItemUpdated(item, getLoggedInPrincipal()?.toRef(), clock.millis())
    return item
  }

  @RetryTransaction
  fun importItems(items: List<Item>, idempotencyKey: UUID, csv: String): BulkResult = idempotencyService.execute(
    idempotencyKey,
    IdempotentOperationType.ITEM_IMPORT,
    buildFingerprint(IdempotentOperationType.ITEM_IMPORT, csv),
    BulkResult::class
  ) {
    val saved = try {
      itemRepository.saveAll(items.map { it.copy(id = null, version = 0) }).also { itemRepository.flush() }
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("A termék név már használatban van!")
    }
    saved.forEach { eventService.logItemCreated(it, getLoggedInPrincipal()?.toRef(), clock.millis()) }
    BulkResult(saved.size)
  }.value

  @Secured(PermissionName.SELL_ITEMS)
  fun processSaleAuthorized(order: Order, line: OrderTerminalController.OrderLineDto) {
    validateOrderLine(line)
    val item = line.itemId?.let { findActiveItem(itemId = it) }
    if (item != null) {
      sellItem(order, item, line.message, line.itemCount)
    } else {
      sellCustomItem(order, line.message, line.itemCount, line.paidAmount!!)
    }
  }

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun removeFromStock(itemId: Int, itemCount: Int) {
    val item = findActiveItem(itemId)
    removeFromStock(item, itemCount)
  }

  /** Variant that takes an already-loaded item — avoids a redundant DB lookup. */
  fun removeFromStock(item: Item, itemCount: Int) {
    if (item.stock < itemCount) throw BadRequestException("A tranzakciót nem lehet feldolgozni, nincs elég a termékből!")
    itemRepository.save(item.copy(stock = item.stock - itemCount))
  }

  fun findActiveItem(itemId: Int): Item {
    val item = itemRepository.findById(itemId).orElseThrow { BadRequestException("A termék nem létezik!") }
    if (!item.enabled) throw BadRequestException("A termék nem elérhető!")
    return item
  }

  fun sellItem(order: Order, item: Item, message: String?, count: Int) {
    removeFromStock(item, count)  // pass already-loaded item — avoids redundant DB lookup

    val amount: BigInteger = item.cost.multiply(BigInteger.valueOf(count.toLong()))
    val amountLong = amount.toLong()
    if (amountLong > 0) {
      accountBalanceService.pay(order.accountId, amountLong, logEvent = false)
    }

    val orderLine = orderLineService.save(
      OrderLine(
        id = null,
        orderId = order.id,
        itemId = item.id,
        itemCount = count,
        message = message,
        usedVoucher = false,
        paidAmount = amount
      )
    )
    if (amountLong > 0) {
      transactionService.recordItemSold(order.id,
        orderLine.id,
        order.accountId,
        item.name,
        message,
        amountLong,
        count,
        clock.millis())
      eventService.logItemSold(order.id,
        order.accountId,
        item.name,
        message,
        amountLong,
        count,
        getLoggedInPrincipal()?.toRef(),
        clock.millis())
    }
  }

  fun sellCustomItem(order: Order, message: String?, count: Int, cost: Long) {
    val amount: BigInteger = BigInteger.valueOf(cost).multiply(BigInteger.valueOf(count.toLong()))
    val amountLong = amount.toLong()
    if (amountLong > 0) {
      accountBalanceService.pay(order.accountId, amountLong, logEvent = false)
    }
    val orderLine = orderLineService.save(
      OrderLine(
        id = null,
        orderId = order.id,
        itemId = null,
        itemCount = count,
        message = message,
        usedVoucher = false,
        paidAmount = amount
      )
    )
    if (amountLong > 0) {
      transactionService.recordItemSold(order.id,
        orderLine.id,
        order.accountId,
        message,
        null,
        amountLong,
        count,
        clock.millis())
      eventService.logItemSold(order.id,
        order.accountId,
        message,
        null,
        amountLong,
        count,
        getLoggedInPrincipal()?.toRef(),
        clock.millis())
    }
  }

  private fun validateOrderLine(line: OrderTerminalController.OrderLineDto) {
    if (line.itemId != null && line.paidAmount != null) throw BadRequestException("Terméknek nem adható egyedi ár!")
    if (line.itemId == null && (line.paidAmount == null || line.paidAmount <= 0L)) throw BadRequestException("Egyedi tétel ára pozitív kell hogy legyen!")
    if (line.itemId == null && line.message == null) throw BadRequestException("Nincs megadva adat a tételről!")
    if (line.itemCount <= 0) throw BadRequestException("Legalább egy egységnyit szükséges megadni a tételből!")
  }

}
