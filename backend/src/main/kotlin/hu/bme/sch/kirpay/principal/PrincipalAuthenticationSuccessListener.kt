package hu.bme.sch.kirpay.principal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.stereotype.Component
import java.time.Clock


@Component
class PrincipalAuthenticationSuccessListener(
    private val events: ApplicationEventPublisher,
    private val clock: Clock
) {

    @EventListener
    fun on(event: AuthenticationSuccessEvent) {
        val principal = event.authentication.principal as? Principal ?: return
        events.publishEvent(PrincipalAuthenticatedEvent(principal.toRef(), clock.millis()))
    }
}
