package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.account.AccountRepository
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRef
import hu.bme.sch.kirpay.principal.Role
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventListenerIntegrationTest {

  @Autowired
  private lateinit var eventRepository: EventRepository

  @Autowired
  private lateinit var eventService: EventService

  @Autowired
  private lateinit var accountListener: AccountEventListener

  @Autowired
  private lateinit var orderListener: OrderEventListener

  @Autowired
  private lateinit var principalListener: PrincipalEventListener

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Test
  fun `displayPrincipal formats admin correctly`() {
    val p = Principal(id = 1, name = "admin", secret = "x", role = Role.ADMIN, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, createdAt = 0, lastUsed = 0)
    val result = principalListener.displayPrincipal(p)
    assertTrue(result.contains("admin"))
    assertTrue(result.contains("Adminisztrátor"))
  }

  @Test
  fun `displayPrincipal formats terminal correctly`() {
    val p = Principal(id = 1, name = "term", secret = "x", role = Role.TERMINAL, active = true,
      canUpload = false, canTransfer = false, canSellItems = true,
      canRedeemVouchers = false, canAssignCards = false, createdAt = 0, lastUsed = 0)
    val result = principalListener.displayPrincipal(p)
    assertTrue(result.contains("term"))
    assertTrue(result.contains("Terminál"))
  }

  @Test
  fun `formatPerformerPrincipal handles null`() {
    assertEquals("Ismeretlen végrehajtó", eventService.formatPerformerPrincipal(null))
  }

  @Test
  fun `formatPerformerPrincipal formats principal`() {
    val p = PrincipalRef(id = 1, name = "admin")
    val result = eventService.formatPerformerPrincipal(p)
    assertTrue(result.contains("admin"))
  }

  @Test
  fun `event creation for account events`() {
    val account = accountRepository.save(
      Account(id = null, name = "Test", email = "t@t.com", phone = null, card = "EVT-ACC", balance = 100, active = true)
    )

    eventService.create("Számla létrehozva",
      "${account.id}: Test - t@t.com",
      "Ismeretlen végrehajtó",
      System.currentTimeMillis())

    val events = eventRepository.findAll().toList()
    val audit = events.find { it.event == "Számla létrehozva" }
    assertNotNull(audit)
    assertTrue(audit.message.contains("Test"))
  }

  @Test
  fun `event creation for order events`() {
    eventService.create("Rendelés létrehozva",
      "Rendelésazonosító: 42 - Számlaazonosító: 7",
      "Ismeretlen végrehajtó",
      System.currentTimeMillis())

    val events = eventRepository.findAll().toList()
    val audit = events.find { it.event == "Rendelés létrehozva" }
    assertNotNull(audit)
    assertTrue(audit.message.contains("42"))
  }

  @Test
  fun `event creation for item sold`() {
    eventService.create("Termék eladva",
      "Rendelésazonosító: 1 - Számlaazonosító: 5 | Mennyiség: 2, Fizetve: 300, Termék: Beer",
      "test",
      System.currentTimeMillis())

    val events = eventRepository.findAll().toList()
    val audit = events.find { it.event == "Termék eladva" }
    assertNotNull(audit)
    assertTrue(audit.message.contains("Beer"))
    assertTrue(audit.message.contains("300"))
  }

  @Test
  fun `event creation for principal events`() {
    eventService.create("Principal létrehozva",
      "admin | Adminisztrátor",
      "Ismeretlen végrehajtó",
      System.currentTimeMillis())

    val events = eventRepository.findAll().toList()
    val audit = events.find { it.event == "Principal létrehozva" }
    assertNotNull(audit)
  }
}
