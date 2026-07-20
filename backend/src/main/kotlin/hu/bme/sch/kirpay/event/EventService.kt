package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.principal.PrincipalRef
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventService(private val eventRepository: EventRepository) {

  fun save(event: Event) = eventRepository.save(event)


  fun findAll() = eventRepository.findAllOrderByTimestampDesc().toList()


  fun findPaginated(page: Int, pageSize: Int) =
    eventRepository.findAllOrderByTimestampDescPaginated(page.toLong() * pageSize, pageSize)


  fun create(event: String, message: String, performedBy: String, timestamp: Long) {
    eventRepository.save(
      Event(
        id = null,
        event = event,
        timestamp = timestamp,
        message = message,
        performedBy = performedBy
      )
    )
  }


  fun formatPerformerPrincipal(by: PrincipalRef?): String =
    by?.let { "${it.name}" } ?: "Ismeretlen végrehajtó"


}
