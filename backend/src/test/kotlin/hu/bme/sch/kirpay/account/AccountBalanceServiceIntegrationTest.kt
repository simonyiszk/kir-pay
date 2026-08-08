package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.transaction.TransactionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class AccountBalanceServiceIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var balanceService: AccountBalanceService

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @Test
  fun `pay deducts balance and creates transaction`() {
    val account = createAccount(card = "PAY-CARD", balance = 1000)

    val result = balanceService.pay("PAY-CARD", 300, UUID.randomUUID(), logEvent = true)

    assertEquals(java.math.BigInteger.valueOf(700), result.account.balance)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(java.math.BigInteger.valueOf(700), reloaded.balance)
  }

  @Test
  fun `pay with insufficient balance throws`() {
    createAccount(card = "POOR-CARD", balance = 50)

    assertThrows<BadRequestException> {
      balanceService.pay("POOR-CARD", 100, UUID.randomUUID(), logEvent = true)
    }

    val account = accountRepository.findActiveAccountByCard("POOR-CARD")!!
    assertEquals(java.math.BigInteger.valueOf(50), account.balance)
  }

  @Test
  fun `pay by account id works`() {
    val account = createAccount(card = null, balance = 500)

    val result = balanceService.pay(accountId = account.id!!, amount = 200L, logEvent = true)

    assertEquals(java.math.BigInteger.valueOf(300), result.balance)
  }

  @Test
  fun `upload adds balance`() {
    val account = createAccount(card = "UPLOAD-CARD", balance = 200)

    val result = balanceService.upload("UPLOAD-CARD", 500, UUID.randomUUID())

    assertEquals(java.math.BigInteger.valueOf(700), result.account.balance)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(java.math.BigInteger.valueOf(700), reloaded.balance)
  }

  @Test
  fun `upload with negative amount throws`() {
    createAccount(card = "NEG-CARD", balance = 100)

    assertThrows<BadRequestException> {
      balanceService.upload("NEG-CARD", -50, UUID.randomUUID())
    }
  }

  @Test
  fun `upload to unknown card throws`() {
    assertThrows<BadRequestException> {
      balanceService.upload("NOBODY", 100, UUID.randomUUID())
    }
  }

  @Test
  fun `transfer moves balance between accounts`() {
    val sender = createAccount(card = "SEND-CARD", balance = 1000, name = "Sender")
    val recipient = createAccount(card = "RECV-CARD", balance = 200, name = "Recipient")

    val result = balanceService.transfer("SEND-CARD", "RECV-CARD", 300, UUID.randomUUID())

    assertEquals(java.math.BigInteger.valueOf(700), result.account.balance)

    val reloadedSender = accountRepository.findById(sender.id!!).get()
    val reloadedRecipient = accountRepository.findById(recipient.id!!).get()
    assertEquals(java.math.BigInteger.valueOf(700), reloadedSender.balance)
    assertEquals(java.math.BigInteger.valueOf(500), reloadedRecipient.balance)
  }

  @Test
  fun `transfer with insufficient balance throws`() {
    createAccount(card = "SEND-POOR", balance = 50)
    createAccount(card = "RECV-RICH", balance = 1000)

    assertThrows<BadRequestException> {
      balanceService.transfer("SEND-POOR", "RECV-RICH", 100, UUID.randomUUID())
    }
  }

  @Test
  fun `transfer to self throws`() {
    createAccount(card = "SELF-CARD", balance = 500)

    assertThrows<BadRequestException> {
      balanceService.transfer("SELF-CARD", "SELF-CARD", 100, UUID.randomUUID())
    }
  }

  @Test
  fun `multiple operations maintain correct balance`() {
    val account = createAccount(card = "MULTI-CARD", balance = 1000)

    balanceService.upload("MULTI-CARD", 500, UUID.randomUUID())
    balanceService.pay("MULTI-CARD", 300, UUID.randomUUID(), logEvent = true)
    balanceService.pay("MULTI-CARD", 200, UUID.randomUUID(), logEvent = true)
    balanceService.upload("MULTI-CARD", 50, UUID.randomUUID())

    val final = accountRepository.findById(account.id!!).get()
    assertEquals(java.math.BigInteger.valueOf(1050), final.balance)
  }
}
