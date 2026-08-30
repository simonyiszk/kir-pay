package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.testIdempotencyService
import hu.bme.sch.kirpay.testPrincipal
import hu.bme.sch.kirpay.testPrincipalDto
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class PrincipalServiceTest {

  private val principalRepository: PrincipalRepository = mockk()
  private val eventService: EventService = mockk(relaxed = true)
  private val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: PrincipalService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = PrincipalService(principalRepository, eventService, passwordEncoder, clock,
      testIdempotencyService(clock))
  }

  @Test
  fun `findAll returns all principals ordered by name`() {
    val principals = listOf(
      testPrincipal(id = 1, name = "Alice"),
      testPrincipal(id = 2, name = "Bob")
    )
    every { principalRepository.findAllOrderByName() } returns principals

    val result = service.findAll()

    assertEquals(2, result.size)
    assertEquals("Alice", result[0].name)
  }

  @Test
  fun `find existing principal returns it`() {
    val principal = testPrincipal(id = 1)
    every { principalRepository.findById(1) } returns Optional.of(principal)

    val result = service.find(1)

    assertEquals(1, result.id)
  }

  @Test
  fun `find non-existent principal throws BadRequestException`() {
    every { principalRepository.findById(999) } returns Optional.empty()

    assertThrows<BadRequestException> { service.find(999) }
  }

  @Test
  fun `createPrincipal with unique name saves and publishes event`() {
    val dto = testPrincipalDto(name = "new-terminal")
    every { principalRepository.findByName("new-terminal") } returns null
    every { principalRepository.save(any()) } answers { firstArg() }

    val result = service.createPrincipal(dto)

    assertNotNull(result)
    assertEquals("new-terminal", result.name)
    verify { eventService.logPrincipalCreated(any(), any(), any()) }
  }

  @Test
  fun `createPrincipal with duplicate name throws BadRequestException when failOnCollision true`() {
    val dto = testPrincipalDto(name = "existing")
    every { principalRepository.findByName("existing") } returns testPrincipal(name = "existing")

    assertThrows<BadRequestException> {
      service.createPrincipal(dto, failOnCollision = true)
    }
  }

  @Test
  fun `createPrincipal with duplicate name updates when failOnCollision false`() {
    val dto = testPrincipalDto(name = "existing", password = "newpw")
    val existing = testPrincipal(name = "existing", id = 1, secret = "old-hash")
    every { principalRepository.findByName("existing") } returns existing
    every { principalRepository.save(any()) } answers { firstArg() }

    val result = service.createPrincipal(dto, failOnCollision = false)

    assertNotNull(result)
    assertEquals(1, result.id)
    assertEquals("existing", result.name)
    verify { eventService.logPrincipalUpdated(any(), any(), any()) }
  }

  @Test
  fun `updatePrincipal preserves createdAt and lastUsed`() {
    val originalCreatedAt = 1690000000000L
    val originalLastUsed = 1690000001000L
    val existing = testPrincipal(
      id = 1,
      name = "old-name",
      createdAt = originalCreatedAt,
      lastUsed = originalLastUsed
    )
    val dto = testPrincipalDto(name = "new-name", password = "***")

    every { principalRepository.findById(1) } returns Optional.of(existing)
    every { principalRepository.findByName("new-name") } returns null
    every { principalRepository.save(any()) } answers { firstArg() }

    val result = service.updatePrincipal(1, dto)

    assertEquals(originalCreatedAt, result.createdAt, "createdAt should be preserved")
    assertEquals(originalLastUsed, result.lastUsed, "lastUsed should be preserved")
  }

  @Test
  fun `updatePrincipal rename to existing name throws BadRequestException`() {
    val existing = testPrincipal(id = 1, name = "terminal-a")
    val collision = testPrincipal(id = 2, name = "terminal-b")
    val dto = testPrincipalDto(name = "terminal-b", password = "***")

    every { principalRepository.findById(1) } returns Optional.of(existing)
    every { principalRepository.findByName("terminal-b") } returns collision

    assertThrows<BadRequestException> { service.updatePrincipal(1, dto) }
  }

  @Test
  fun `setEnabled disables terminal successfully`() {
    val principal = testPrincipal(id = 1, role = Role.TERMINAL, active = true)
    every { principalRepository.findById(1) } returns Optional.of(principal)
    every { principalRepository.save(any()) } answers { firstArg() }

    val result = service.setEnabled(1, false)

    assertFalse(result.active)
    verify { eventService.logPrincipalUpdated(any(), any(), any()) }
  }

  @Test
  fun `setEnabled cannot disable admin`() {
    val admin = testPrincipal(id = 1, role = Role.ADMIN, active = true)
    every { principalRepository.findById(1) } returns Optional.of(admin)

    assertThrows<BadRequestException> { service.setEnabled(1, false) }
  }

  @Test
  fun `delete terminal succeeds`() {
    val principal = testPrincipal(id = 1, role = Role.TERMINAL)
    every { principalRepository.findById(1) } returns Optional.of(principal)
    every { principalRepository.delete(any()) } just Runs

    service.delete(1)

    verify { principalRepository.delete(principal) }
    verify { eventService.logPrincipalDeleted(any(), any(), any()) }
  }

  @Test
  fun `delete admin throws BadRequestException`() {
    val admin = testPrincipal(id = 1, role = Role.ADMIN)
    every { principalRepository.findById(1) } returns Optional.of(admin)

    assertThrows<BadRequestException> { service.delete(1) }
  }

  @Test
  fun `updateLastUsed calls repository method`() {
    every { principalRepository.updateLastUsed(1, any()) } returns Unit

    service.updateLastUsed(1)

    verify { principalRepository.updateLastUsed(1, 1700000000000L) }
  }

  @Test
  fun `updateLastUsed with null id is no-op`() {
    service.updateLastUsed(null)

    verify(exactly = 0) { principalRepository.updateLastUsed(any(), any()) }
  }
}
