package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.testAccount
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals

class AccountBalanceServiceTest {

  private val accountRepository: AccountRepository = mockk()
  private val events: ApplicationEventPublisher = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: AccountBalanceService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = AccountBalanceService(accountRepository, events, clock)
  }

  // --- pay(card, ...) ---

  @Test
  fun `pay with valid card deducts balance and publishes event`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 1000)
    every { accountRepository.findActiveAccountByCard(card) } returns account
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.pay(card, 300, logEvent = true)

    assertEquals(700, result.balance)
    verify { accountRepository.save(match { it.balance == 700L }) }
    verify { events.publishEvent(any<AccountPayEvent>()) }
  }

  @Test
  fun `pay with insufficient balance throws BadRequestException`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 100)
    every { accountRepository.findActiveAccountByCard(card) } returns account

    assertThrows<BadRequestException> {
      service.pay(card, 500, logEvent = true)
    }
    verify(exactly = 0) { accountRepository.save(any()) }
  }

  @Test
  fun `pay with negative amount throws BadRequestException`() {
    assertThrows<BadRequestException> {
      service.pay("CARD-001", -100, logEvent = true)
    }
  }

  @Test
  fun `pay with unknown card throws BadRequestException`() {
    every { accountRepository.findActiveAccountByCard("UNKNOWN") } returns null

    assertThrows<BadRequestException> {
      service.pay("UNKNOWN", 100, logEvent = true)
    }
  }

  @Test
  fun `pay by accountId with negative amount throws BadRequestException`() {
    assertThrows<BadRequestException> {
      service.pay(accountId = 1, amount = -50, logEvent = true)
    }
  }


  @Test
  fun `pay with zero amount should be rejected`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 1000)
    every { accountRepository.findActiveAccountByCard(card) } returns account

    assertThrows<BadRequestException> {
      service.pay(card, 0, logEvent = true)
    }
  }

  // --- upload ---

  @Test
  fun `upload adds balance and publishes event`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 500)
    every { accountRepository.findActiveAccountByCard(card) } returns account
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.upload(card, 200)

    assertEquals(700, result.balance)
    verify { accountRepository.save(match { it.balance == 700L }) }
    verify { events.publishEvent(any<AccountUploadEvent>()) }
  }

  @Test
  fun `upload with negative amount throws BadRequestException`() {
    assertThrows<BadRequestException> {
      service.upload("CARD-001", -100)
    }
  }

  @Test
  fun `upload with zero amount should be rejected`() {
    assertThrows<BadRequestException> {
      service.upload("CARD-001", 0)
    }
  }

  @Test
  fun `upload with Long_MAX_VALUE to account with positive balance should fail cleanly`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 1)
    every { accountRepository.findActiveAccountByCard(card) } returns account

    assertThrows<BadRequestException> {
      service.upload(card, Long.MAX_VALUE)
    }
  }

  // --- transfer ---

  @Test
  fun `transfer moves balance between accounts`() {
    val senderCard = "CARD-001"
    val recipientCard = "CARD-002"
    val sender = testAccount(id = 1, card = senderCard, balance = 1000)
    val recipient = testAccount(id = 2, card = recipientCard, balance = 200)

    every { accountRepository.findActiveAccountByCard(senderCard) } returns sender
    every { accountRepository.findActiveAccountByCard(recipientCard) } returns recipient
    every { accountRepository.save(any()) } answers { firstArg() }

    val result = service.transfer(senderCard, recipientCard, 300)

    assertEquals(700, result.balance) // sender
    verify { accountRepository.save(match { it.id == 1 && it.balance == 700L }) }
    verify { accountRepository.save(match { it.id == 2 && it.balance == 500L }) }
    verify { events.publishEvent(any<AccountBalanceTransferEvent>()) }
  }

  @Test
  fun `transfer to self throws BadRequestException`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 1000)

    every { accountRepository.findActiveAccountByCard(card) } returns account

    assertThrows<BadRequestException> {
      service.transfer(card, card, 100)
    }
  }

  @Test
  fun `transfer with insufficient balance throws BadRequestException`() {
    val senderCard = "CARD-001"
    val recipientCard = "CARD-002"
    val sender = testAccount(id = 1, card = senderCard, balance = 50)
    val recipient = testAccount(id = 2, card = recipientCard, balance = 200)

    every { accountRepository.findActiveAccountByCard(senderCard) } returns sender
    every { accountRepository.findActiveAccountByCard(recipientCard) } returns recipient

    assertThrows<BadRequestException> {
      service.transfer(senderCard, recipientCard, 100)
    }
  }

  @Test
  fun `transfer with zero amount should be rejected`() {
    val sender = testAccount(id = 1, card = "CARD-001", balance = 1000)
    val recipient = testAccount(id = 2, card = "CARD-002", balance = 200)

    every { accountRepository.findActiveAccountByCard("CARD-001") } returns sender
    every { accountRepository.findActiveAccountByCard("CARD-002") } returns recipient

    assertThrows<BadRequestException> {
      service.transfer("CARD-001", "CARD-002", 0)
    }
  }
}
