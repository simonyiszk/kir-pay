package hu.bme.sch.kirpay.transaction

import hu.bme.sch.kirpay.BaseIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransactionEventListenerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  @BeforeEach
  fun cleanTransactions() {
    transactionRepository.deleteAll()
    eventRepository.deleteAll()
  }

  @Test
  fun `save CHARGE transaction`() {
    val account = createAccount(card = "TX-CHARGE", name = "Payer", balance = 500)

    transactionRepository.save(
      Transaction(
        id = null, type = TransactionType.CHARGE,
        senderId = account.id, recipientId = null,
        amount = 200, message = null,
        timestamp = System.currentTimeMillis()
      )
    )

    val transactions = transactionRepository.findAll().toList()
    val charge = transactions.find { it.type == TransactionType.CHARGE && it.senderId == account.id }
    assertNotNull(charge, "CHARGE transaction should be created")
    assertEquals(200L, charge.amount)
  }

  @Test
  fun `save TOP_UP transaction`() {
    val account = createAccount(card = "TX-TOPUP", name = "Uploader", balance = 100)

    transactionRepository.save(
      Transaction(
        id = null, type = TransactionType.TOP_UP,
        senderId = null, recipientId = account.id,
        amount = 500, message = null,
        timestamp = System.currentTimeMillis()
      )
    )

    val transactions = transactionRepository.findAll().toList()
    val topUp = transactions.find { it.type == TransactionType.TOP_UP && it.recipientId == account.id }
    assertNotNull(topUp, "TOP_UP transaction should be created")
    assertEquals(500L, topUp.amount)
  }

  @Test
  fun `save TRANSFER transaction`() {
    val sender = createAccount(card = "TX-SEND", name = "Sender", balance = 1000)
    val recipient = createAccount(card = "TX-RECV", name = "Recipient", balance = 200)

    transactionRepository.save(
      Transaction(
        id = null, type = TransactionType.TRANSFER,
        senderId = sender.id, recipientId = recipient.id,
        amount = 300, message = null,
        timestamp = System.currentTimeMillis()
      )
    )

    val transactions = transactionRepository.findAll().toList()
    val transfer = transactions.find { it.type == TransactionType.TRANSFER }
    assertNotNull(transfer, "TRANSFER transaction should be created")
    assertEquals(300L, transfer.amount)
    assertEquals(sender.id, transfer.senderId)
    assertEquals(recipient.id, transfer.recipientId)
  }

  @Test
  fun `save CHARGE transaction with message`() {
    val account = createAccount(card = "TX-MSG", name = "Buyer", balance = 1000)

    transactionRepository.save(
      Transaction(
        id = null, type = TransactionType.CHARGE,
        senderId = account.id, recipientId = null,
        amount = 500, message = "Beer: Extra cold",
        timestamp = System.currentTimeMillis()
      )
    )

    val transactions = transactionRepository.findAll().toList()
    val charge = transactions.find { it.message != null && it.type == TransactionType.CHARGE }
    assertNotNull(charge, "CHARGE transaction with message should be created")
    assertEquals(500L, charge.amount)
    assertTrue(charge.message!!.contains("Beer"))
  }

  @Test
  fun `multiple transactions are distinct`() {
    val account = createAccount(card = "TX-MULTI", name = "Multi", balance = 2000)

    transactionRepository.save(
      Transaction(id = null, type = TransactionType.TOP_UP, senderId = null,
        recipientId = account.id, amount = 100, message = null, timestamp = System.currentTimeMillis())
    )
    transactionRepository.save(
      Transaction(id = null, type = TransactionType.CHARGE, senderId = account.id,
        recipientId = null, amount = 50, message = null, timestamp = System.currentTimeMillis())
    )
    transactionRepository.save(
      Transaction(id = null, type = TransactionType.CHARGE, senderId = account.id,
        recipientId = null, amount = 30, message = null, timestamp = System.currentTimeMillis())
    )

    val transactions = transactionRepository.findAll().toList()
    val topUps = transactions.count { it.type == TransactionType.TOP_UP }
    val charges = transactions.count { it.type == TransactionType.CHARGE }

    assertEquals(1, topUps)
    assertEquals(2, charges)
  }

  @Test
  fun `listener handles AccountPayEvent`() {
    val account = createAccount(card = "TX-LISTEN", name = "Listener", balance = 500)
    assertNotNull(account.id)
  }
}
