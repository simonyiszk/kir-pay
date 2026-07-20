package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventServiceIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var eventService: EventService

  @Test
  fun `create event persists to database`() {
    eventService.create("TEST_EVENT", "Test message", "test-user", System.currentTimeMillis())

    val all = eventService.findAll()
    assertTrue(all.any { it.event == "TEST_EVENT" })
  }

  @Test
  fun `formatPerformerPrincipal formats correctly`() {
    val result = eventService.formatPerformerPrincipal(null)
    assertEquals("Ismeretlen végrehajtó", result)
  }

  @Test
  fun `findAll returns events ordered by timestamp desc`() {
    val ts = System.currentTimeMillis()
    eventService.create("EVENT_1", "First", "user", ts - 1000)
    eventService.create("EVENT_2", "Second", "user", ts)
    eventService.create("EVENT_3", "Third", "user", ts - 2000)

    val all = eventService.findAll()
    val events = all.filter { it.event.startsWith("EVENT_") }
    assertEquals("EVENT_2", events[0].event) // Most recent first
    assertEquals("EVENT_1", events[1].event)
    assertEquals("EVENT_3", events[2].event)
  }

  @Test
  fun `findPaginated respects page size`() {
    val ts = System.currentTimeMillis()
    repeat(5) { eventService.create("PAGE_$it", "Event $it", "user", ts + it * 1000) }

    val page = eventService.findPaginated(0, 2)
    assertEquals(2, page.size)
  }

  @Test
  fun `save event entity directly`() {
    val event = Event(id = null,
      event = "DIRECT",
      timestamp = System.currentTimeMillis(),
      message = "Direct save",
      performedBy = "test")
    val saved = eventService.save(event)
    assertNotNull(saved.id)

    val found = eventRepository.findById(saved.id!!)
    assertTrue(found.isPresent)
    assertEquals("DIRECT", found.get().event)
  }
}
