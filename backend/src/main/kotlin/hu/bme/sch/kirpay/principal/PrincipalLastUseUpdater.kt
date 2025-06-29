package hu.bme.sch.kirpay.principal

import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.ApplicationModuleListener

@Configuration
class PrincipalLastUseUpdater(private val principalService: PrincipalService) {

  @ApplicationModuleListener
  fun on(event: PrincipalAuthenticatedEvent) {
    principalService.updateLastUsed(event.principal.id)
  }

}
