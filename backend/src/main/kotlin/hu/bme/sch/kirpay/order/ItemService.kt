package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountBalanceService
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.principal.PermissionName
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class ItemService(
  private val accountBalanceService: AccountBalanceService,
  private val itemRepository: ItemRepository,
  private val orderLineService: OrderLineService,
  private val events: ApplicationEventPublisher,
  private val clock: Clock
) {

  fun find(id: Int): Item = itemRepository.findById(id).orElseThrow { BadRequestException("A termék nem létezik!") }


  fun findAll() = itemRepository.findAllOrderByName()


  fun findAllActive(): List<Item> = itemRepository.findByEnabledOrderByName(true)


  fun findPaginated(page: Int, size: Int) = itemRepository.findAllOrderByNamePaginated(page.toLong() * size, size)


  fun createItem(dto: Item): Item {
    val item = itemRepository.save(dto)
    events.publishEvent(ItemCreatedEvent(item, getLoggedInPrincipal(), clock.millis()))
    return item
  }


  fun updateItem(id: Int, dto: Item): Item {
    if (!itemRepository.existsById(id)) throw BadRequestException("A termék nem létezik!")
    val item = itemRepository.save(dto.copy(id = id))
    events.publishEvent(ItemUpdatedEvent(item, getLoggedInPrincipal(), clock.millis()))
    return item
  }


  fun deleteItem(itemId: Int) {
    val item = find(itemId)
    itemRepository.deleteById(itemId)
    events.publishEvent(ItemDeletedEvent(item, getLoggedInPrincipal(), clock.millis()))
  }

  fun setEnabled(itemId: Int, enabled: Boolean): Item {
    val item = itemRepository.save(find(itemId).copy(enabled = enabled))
    events.publishEvent(ItemUpdatedEvent(item, getLoggedInPrincipal(), clock.millis()))
    return item
  }


  fun importItems(items: List<Item>): Unit = itemRepository.saveAll(items.map { it.copy(id = null) })
    .forEach { events.publishEvent(ItemCreatedEvent(it, getLoggedInPrincipal(), clock.millis())) }


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


  fun removeFromStock(itemId: Int, itemCount: Int) {
    val item = findActiveItem(itemId)
    if (item.stock < itemCount) throw BadRequestException("A tranzakciót nem lehet feldolgozni, nincs elég a termékből!")
    itemRepository.save(item.copy(stock = item.stock - itemCount))
  }


  fun findActiveItem(itemId: Int): Item {
    val item = itemRepository.findById(itemId).orElseThrow { BadRequestException("A termék nem létezik!") }
    if (!item.enabled) throw BadRequestException("A termék nem elérhető!")
    return item
  }


  fun sellItem(order: Order, item: Item, message: String?, count: Int) {
    removeFromStock(item.id!!, count)

    val amount = item.cost * count
    accountBalanceService.pay(order.accountId, amount, logEvent = false)

    orderLineService.save(
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
    events.publishEvent(
      ItemSoldEvent(
        order.id,
        order.accountId,
        item.name,
        message,
        amount,
        count,
        getLoggedInPrincipal(),
        clock.millis()
      )
    )
  }

  fun sellCustomItem(order: Order, message: String?, count: Int, cost: Long) {
    val amount = cost * count
    accountBalanceService.pay(order.accountId, amount, logEvent = false)
    orderLineService.save(
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
    events.publishEvent(
      ItemSoldEvent(
        order.id,
        order.accountId,
        message,
        null,
        amount,
        count,
        getLoggedInPrincipal(),
        clock.millis()
      )
    )
  }


  private fun validateOrderLine(line: OrderTerminalController.OrderLineDto) {
    if (line.itemId != null && line.paidAmount != null) throw BadRequestException("Terméknek nem adható egyedi ár!")
    if (line.itemId == null && (line.paidAmount == null || line.paidAmount <= 0L)) throw BadRequestException("Egyedi tétel ára pozitív kell hogy legyen!")
    if (line.itemId == null && line.message == null) throw BadRequestException("Nincs megadva adat a tételről!")
    if (line.itemCount <= 0) throw BadRequestException("Legalább egy egységnyit szükséges megadni a tételből!")
  }

}
