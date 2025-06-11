package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.order.*
import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.ApplicationModuleListener

@Configuration
class OrderEventListener(private val eventService: EventService) {

  @ApplicationModuleListener
  fun on(event: OrderCreatedEvent) {
    eventService.create(
      "Rendelés létrehozva",
      "Rendelésazonosító: ${event.orderId} - Számlaazonosító: ${event.accountId}",
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: ItemSoldEvent) {
    val item = ", Termék: ${event.item}"
    val message = event.message?.let { ", Üzenet: $it" } ?: ""
    val saleNumbers = "Mennyiség: ${event.count}, Fizetve: ${event.amount}"
    eventService.create(
      "Termék eladva",
      "Rendelésazonosító: ${event.orderId} - Számlaazonosító: ${event.accountId} | " + saleNumbers + item + message,
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: VoucherRedeemedEvent) {
    eventService.create(
      "Utalvány beváltva",
      "Rendelésazonosító: ${event.orderId} - Számlaazonosító: ${event.accountId}",
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: ItemCreatedEvent) {
    eventService.create(
      "Termék létrehozva",
      displayItem(event.item),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: ItemUpdatedEvent) {
    eventService.create(
      "Termék módosítva",
      displayItem(event.item),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: ItemDeletedEvent) {
    eventService.create(
      "Termék törölve",
      displayItem(event.item),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: VoucherCreatedEvent) {
    eventService.create(
      "Utalvány Létrehozva",
      displayVoucher(event.voucher),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: VoucherUpdatedEvent) {
    eventService.create(
      "Utalvány módosítva",
      displayVoucher(event.voucher),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: VoucherDeletedEvent) {
    eventService.create(
      "Utalvány törölve",
      displayVoucher(event.voucher),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  private fun displayItem(item: Item) =
    "${item.name}: ${item.stock} db, ${item.cost} JMF - ${if (item.enabled) "Aktív" else "Inaktív"}"


  private fun displayVoucher(voucher: Voucher) =
    "Számlaazonosító: ${voucher.accountId}, Termékazonosító: ${voucher.itemId}, Darabszám: ${voucher.count}"

}
