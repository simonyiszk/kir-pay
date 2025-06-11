package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.principal.*
import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.ApplicationModuleListener


@Configuration
class PrincipalEventListener(private val eventService: EventService) {

  @ApplicationModuleListener
  fun on(event: PrincipalCreatedEvent) {
    eventService.create(
      "Principal létrehozva",
      displayPrincipal(event.who),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: PrincipalUpdatedEvent) {
    eventService.create(
      "Principal módosítva",
      displayPrincipal(event.who),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  @ApplicationModuleListener
  fun on(event: PrincipalDeletedEvent) {
    eventService.create(
      "Principal törölve",
      displayPrincipal(event.who),
      eventService.formatPerformerPrincipal(event.by),
      event.timestamp
    )
  }


  fun displayPrincipal(principal: Principal) =
    "${principal.name} | ${
      when (principal.role) {
        Role.ADMIN -> "Adminisztrátor"
        Role.TERMINAL -> "Terminál"
      }
    }"

}
