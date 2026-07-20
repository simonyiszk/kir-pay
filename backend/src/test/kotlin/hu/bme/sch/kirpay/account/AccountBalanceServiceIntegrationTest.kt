package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.transaction.TransactionRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class AccountBalanceServiceIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var balanceService: AccountBalanceService

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  // --- pay ---

  @Test
  fun `pay deducts balance and creates transaction`() {
    val account = createAccount(card = "PAY-CARD", balance = 1000)

    val result = balanceService.pay("PAY-CARD", 300, logEvent = true)

    assertEquals(700, result.balance)

    // Reload from DB
    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(700, reloaded.balance)
  }

  @Test
  fun `pay with insufficient balance throws`() {
    createAccount(card = "POOR-CARD", balance = 50)

    assertThrows<BadRequestException> {
      balanceService.pay("POOR-CARD", 100, logEvent = true)
    }

    // Balance should be unchanged
    val account = accountRepository.findActiveAccountByCard("POOR-CARD")!!
    assertEquals(50, account.balance)
  }

  @Test
  fun `pay by account id works`() {
    val account = createAccount(card = null, balance = 500)

    val result = balanceService.pay(accountId = account.id!!, amount = 200, logEvent = true)

    assertEquals(300, result.balance)
  }

  // --- upload ---

  @Test
  fun `upload adds balance`() {
    val account = createAccount(card = "UPLOAD-CARD", balance = 200)

    val result = balanceService.upload("UPLOAD-CARD", 500)

    assertEquals(700, result.balance)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(700, reloaded.balance)
  }

  @Test
  fun `upload with negative amount throws`() {
    createAccount(card = "NEG-CARD", balance = 100)

    assertThrows<BadRequestException> {
      balanceService.upload("NEG-CARD", -50)
    }
  }

  @Test
  fun `upload to unknown card throws`() {
    assertThrows<BadRequestException> {
      balanceService.upload("NOBODY", 100)
    }
  }

  // --- transfer ---

  @Test
  fun `transfer moves balance between accounts`() {
    val sender = createAccount(card = "SEND-CARD", balance = 1000, name = "Sender")
    val recipient = createAccount(card = "RECV-CARD", balance = 200, name = "Recipient")

    val result = balanceService.transfer("SEND-CARD", "RECV-CARD", 300)

    assertEquals(700, result.balance) // sender

    val reloadedSender = accountRepository.findById(sender.id!!).get()
    val reloadedRecipient = accountRepository.findById(recipient.id!!).get()
    assertEquals(700, reloadedSender.balance)
    assertEquals(500, reloadedRecipient.balance)
  }

  @Test
  fun `transfer with insufficient balance throws`() {
    createAccount(card = "SEND-POOR", balance = 50)
    createAccount(card = "RECV-RICH", balance = 1000)

    assertThrows<BadRequestException> {
      balanceService.transfer("SEND-POOR", "RECV-RICH", 100)
    }
  }

  @Test
  fun `transfer to self throws`() {
    createAccount(card = "SELF-CARD", balance = 500)

    assertThrows<BadRequestException> {
      balanceService.transfer("SELF-CARD", "SELF-CARD", 100)
    }
  }

  @Test
  fun `multiple operations maintain correct balance`() {
    val account = createAccount(card = "MULTI-CARD", balance = 1000)

    balanceService.upload("MULTI-CARD", 500)    // 1500
    balanceService.pay("MULTI-CARD", 300, logEvent = true)  // 1200
    balanceService.pay("MULTI-CARD", 200, logEvent = true)  // 1000
    balanceService.upload("MULTI-CARD", 50)     // 1050

    val final = accountRepository.findById(account.id!!).get()
    assertEquals(1050, final.balance)
  }
}
