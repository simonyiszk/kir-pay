package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.account.*
import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.ApplicationModuleListener


@Configuration
class AccountEventListener(private val eventService: EventService) {

  @ApplicationModuleListener
  fun on(event: AccountCreatedEvent) {
    eventService.create(
      "Számla létrehozva",
      displayAccount(event.account),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountCardAssignedEvent) {
    eventService.create(
      "Kártya hozzárendelve",
      displayAccount(event.account),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountUpdatedEvent) {
    eventService.create(
      "Számla szerkesztve",
      displayAccount(event.account),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountDeletedEvent) {
    eventService.create(
      "Számla törölve",
      displayAccount(event.account),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountPayEvent) {
    eventService.create(
      "Fizetés",
      "${displayAccount(event.account)} | ${event.amount} JMF",
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountUploadEvent) {
    eventService.create(
      "Feltöltés",
      "${displayAccount(event.account)} | ${event.amount} JMF",
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountBalanceTransferEvent) {
    eventService.create(
      "Átutalás",
      "${displayAccount(event.sender)} -> ${displayAccount(event.recipient)} | ${event.amount} JMF",
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  private fun displayAccount(account: Account) =
    "${account.id}: " + account.name + (account.email?.let { " - $it" } ?: "")

}
