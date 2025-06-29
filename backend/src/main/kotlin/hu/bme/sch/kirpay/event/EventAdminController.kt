package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.common.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ADMIN_API)
class EventAdminController(
  parserFactory: CsvParserFactory,
  private val eventService: EventService
) {
  private val eventParser = parserFactory.getParserForType(Event::class)


  @GetMapping("/events")
  fun getEventsPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      eventService.findAll()
    else
      eventService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/export/events", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportEvents(): ResponseEntity<String> {
    val events = eventService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("events.csv")
      .body(eventParser.toCsv(events))
  }

}
