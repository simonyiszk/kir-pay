package hu.bme.sch.kirpay.principal

import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Configuration
class PrincipalLastUseUpdater(
  private val principalService: PrincipalService,
  private val clock: Clock
) {
  @Transactional
  fun updateIfNeeded(principalRef: PrincipalRef, timestamp: Long) {
    val id = principalRef.id ?: return
    val principal = principalService.find(id)
    if (clock.millis() - principal.lastUsed < 30_000) return
    principalService.updateLastUsed(id)
  }

}
