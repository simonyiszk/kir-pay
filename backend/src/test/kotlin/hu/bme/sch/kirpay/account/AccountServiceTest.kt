package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.common.NotFoundException
import hu.bme.sch.kirpay.testAccount
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountServiceTest {

  private val accountRepository: AccountRepository = mockk()
  private val events: ApplicationEventPublisher = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: AccountService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = AccountService(accountRepository, events, clock)
  }

  // --- find ---

  @Test
  fun `find existing account returns it`() {
    val account = testAccount(id = 1)
    every { accountRepository.findById(1) } returns Optional.of(account)

    val result = service.find(1)

    assertEquals(1, result.id)
    assertEquals("Test Account", result.name)
  }

  @Test
  fun `find non-existent account throws NotFoundException`() {
    every { accountRepository.findById(999) } returns Optional.empty()

    assertThrows<NotFoundException> { service.find(999) }
  }

  // --- create ---

  @Test
  fun `create saves account and publishes event`() {
    val dto =
      AccountCreateDto(id = null, name = "New", email = null, phone = null, card = null, balance = 0, active = true)
    val saved = testAccount(id = 5, name = "New", balance = 0)
    every { accountRepository.save(any()) } returns saved

    val result = service.create(dto)

    assertEquals("New", result.name)
    verify { events.publishEvent(any<AccountCreatedEvent>()) }
  }

  @Test
  fun `create with client-supplied id does not overwrite existing account`() {
    val existingId = 5
    val existing = testAccount(id = existingId, name = "Alice", balance = 500)
    val dto = AccountCreateDto(id = existingId,
      name = "Bob",
      email = null,
      phone = null,
      card = null,
      balance = 0,
      active = true)

    every { accountRepository.save(any()) } answers {
      val a = firstArg<Account>()
      a.copy(id = if (a.id == null) 10 else a.id)
    }

    service.create(dto)

    verify { accountRepository.save(match { it.id == null }) }
  }

  // --- update ---

  @Test
  fun `update existing account publishes event`() {
    val existing = testAccount(id = 1, name = "Old", balance = 500)
    val dto = AccountUpdateDto(name = "Updated", email = null, phone = null, card = null, active = true)

    every { accountRepository.existsById(1) } returns true
    every { accountRepository.findById(1) } returns Optional.of(existing)
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.update(1, dto)

    assertEquals("Updated", result.name)
    assertEquals(500L, result.balance)  // balance preserved from existing
    verify { events.publishEvent(any<AccountUpdatedEvent>()) }
  }

  @Test
  fun `update non-existent account throws BadRequestException`() {
    every { accountRepository.existsById(999) } returns false

    assertThrows<BadRequestException> {
      service.update(999, AccountUpdateDto("X", null, null, null, true))
    }
  }

  // --- setEnabled ---

  @Test
  fun `setEnabled disables account and publishes event`() {
    val account = testAccount(id = 1, active = true)
    every { accountRepository.findById(1) } returns Optional.of(account)
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.setEnabled(1, false)

    assertFalse(result.active)
    verify { events.publishEvent(any<AccountUpdatedEvent>()) }
  }

  // --- deleteAccount ---

  @Test
  fun `deleteAccount deletes and publishes event`() {
    val account = testAccount(id = 1, balance = 0)
    every { accountRepository.findById(1) } returns Optional.of(account)
    every { accountRepository.deleteById(1) } just Runs

    service.deleteAccount(1)

    verify { accountRepository.deleteById(1) }
    verify { events.publishEvent(any<AccountDeletedEvent>()) }
  }

  @Test
  fun `delete account with orders should be rejected`() {
    val account = testAccount(id = 1, balance = 500)
    every { accountRepository.findById(1) } returns Optional.of(account)

    assertThrows<BadRequestException> { service.deleteAccount(1) }
  }

  // --- findActiveByCard ---

  @Test
  fun `findActiveByCard returns active account`() {
    val account = testAccount(id = 1, card = "CARD-OK")
    every { accountRepository.findActiveAccountByCard("CARD-OK") } returns account

    val result = service.findActiveByCard("CARD-OK")

    assertEquals(1, result.id)
  }

  @Test
  fun `findActiveByCard with unknown card throws NotFoundException`() {
    every { accountRepository.findActiveAccountByCard("UNKNOWN") } returns null

    assertThrows<NotFoundException> { service.findActiveByCard("UNKNOWN") }
  }

  // --- assignCard ---

  @Test
  fun `assignCard sets card on account`() {
    val account = testAccount(id = 1, card = null)
    every { accountRepository.findById(1) } returns Optional.of(account)
    every { accountRepository.findByCard("NEW-CARD") } returns null
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.assignCard(1, "NEW-CARD")

    assertEquals("NEW-CARD", result.card)
    verify { events.publishEvent(any<AccountCardAssignedEvent>()) }
  }

  @Test
  fun `assignCard reassigns card from previous holder`() {
    val account = testAccount(id = 1, card = null)
    val previousHolder = testAccount(id = 2, card = "SHARED-CARD")
    every { accountRepository.findById(1) } returns Optional.of(account)
    every { accountRepository.findByCard("SHARED-CARD") } returns previousHolder
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.assignCard(1, "SHARED-CARD")

    assertEquals("SHARED-CARD", result.card)
    // Previous holder should have card set to null
    verify { accountRepository.save(match { it.id == 2 && it.card == null }) }
  }

  @Test
  fun `assignCard with unknown account throws BadRequestException`() {
    every { accountRepository.findById(999) } returns Optional.empty()

    assertThrows<BadRequestException> { service.assignCard(999, "CARD") }
  }

  // --- count + balance ---

  @Test
  fun `countAll returns repository count`() {
    every { accountRepository.count() } returns 42L

    assertEquals(42L, service.countAll())
  }

  @Test
  fun `getAllActiveBalance returns active balance sum`() {
    every { accountRepository.getAllActiveBalance() } returns 5000L

    assertEquals(5000L, service.getAllActiveBalance())
  }

  // --- importAccounts ---

  @Test
  fun `importAccounts strips ids and saves all`() {
    val accounts = listOf(
      testAccount(id = 5, name = "A"),
      testAccount(id = 10, name = "B")
    )
    val savedAccounts = mutableListOf<Account>()
    every { accountRepository.saveAll(any<Iterable<Account>>()) } answers {
      val iterable = firstArg<Iterable<Account>>()
      savedAccounts.addAll(iterable)
      iterable.toList()
    }

    service.importAccounts(accounts)

    assertTrue(savedAccounts.all { it.id == null }, "All imported accounts should have id=null")
  }
}
