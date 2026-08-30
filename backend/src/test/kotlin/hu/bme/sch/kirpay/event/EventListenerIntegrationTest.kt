package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.account.AccountCreateDto
import hu.bme.sch.kirpay.account.AccountRepository
import hu.bme.sch.kirpay.account.AccountService
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRef
import hu.bme.sch.kirpay.principal.PrincipalRepository
import hu.bme.sch.kirpay.principal.Role
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class EventListenerIntegrationTest {

  @Autowired
  private lateinit var eventRepository: EventRepository

  @Autowired
  private lateinit var eventService: EventService

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Autowired
  private lateinit var accountService: AccountService

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()

  @BeforeEach
  fun setUp() {
    eventRepository.deleteAll()
    accountRepository.deleteAll()
    SecurityContextHolder.clearContext()
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
    eventRepository.deleteAll()
    accountRepository.deleteAll()
  }

  @Test
  fun `displayPrincipal formats admin correctly`() {
    val p = Principal(id = 1, name = "admin", secret = "x", role = Role.ADMIN, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, createdAt = 0, lastUsed = 0, version = 0)
    val result = eventService.displayPrincipal(p)
    assertTrue(result.contains("admin"))
    assertTrue(result.contains("Adminisztrátor"))
  }

  @Test
  fun `displayPrincipal formats terminal correctly`() {
    val p = Principal(id = 1, name = "term", secret = "x", role = Role.TERMINAL, active = true,
      canUpload = false, canTransfer = false, canSellItems = true,
      canRedeemVouchers = false, canAssignCards = false, createdAt = 0, lastUsed = 0, version = 0)
    val result = eventService.displayPrincipal(p)
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
      Account(id = null,
        name = "Test",
        email = "t@t.com",
        phone = null,
        card = "EVT-ACC",
        balance = java.math.BigInteger.valueOf(100),
        active = true)
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

  @Test
  fun `AccountCreatedEvent listener creates audit event row`() {
    val terminal = principalRepository.save(Principal(
      id = null, name = "evt-listener-term", secret = encoder.encode("test-pw")!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis(), version = 0
    ))
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(terminal, "test-pw", terminal.authorities)

    accountService.create(AccountCreateDto(id = null,
      name = "AuditAccount",
      email = "audit@test.com",
      phone = null,
      card = null,
      balance = 100,
      active = true,
      idempotencyKey = java.util.UUID.randomUUID()
    ))

    val events = eventRepository.findAll().toList()
    val auditEvent = events.find { it.event == "Számla létrehozva" && it.message.contains("AuditAccount") }
    assertNotNull(auditEvent, "Számla létrehozva audit event should be created synchronously")
    assertTrue(auditEvent.message.contains("audit@test.com"))
  }
}
