package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.common.AppEvent
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener

@Configuration
class AppEventListener(private val eventService: EventService) {
  private val logger = LoggerFactory.getLogger(AppEventListener::class.java)


  @EventListener
  fun on(event: ApplicationStartedEvent) {
    eventService.create("Életciklus", "Az alkalmazás elindult", "Rendszer", event.timestamp)
  }


  @EventListener
  fun on(event: ContextClosedEvent) {
    eventService.create("Életciklus", "Az alkalmazás leállt", "Rendszer", event.timestamp)
  }


  @EventListener
  fun on(event: AppEvent) {
    logger.info("{}", event)
  }

}
