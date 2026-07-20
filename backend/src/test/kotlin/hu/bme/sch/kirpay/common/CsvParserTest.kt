package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.event.Event
import hu.bme.sch.kirpay.order.Order
import hu.bme.sch.kirpay.order.OrderAdminController
import hu.bme.sch.kirpay.order.OrderLine
import hu.bme.sch.kirpay.order.OrderWithOrderLine
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalDto
import hu.bme.sch.kirpay.principal.Role
import hu.bme.sch.kirpay.transaction.Transaction
import hu.bme.sch.kirpay.transaction.TransactionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue


class CsvParserTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var parserFactory: CsvParserFactory

  @Test
  fun `Account CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Account::class)
    val accounts = listOf(
      Account(id = null,
        name = "Alice",
        email = "alice@test.com",
        phone = "+36123",
        card = "C1",
        balance = 100,
        active = true),
      Account(id = null, name = "Bob", email = null, phone = null, card = "C2", balance = 200, active = false)
    )

    val csv = parser.toCsv(accounts)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertEquals("Alice", parsed[0].name)
    assertEquals(100L, parsed[0].balance)
    assertEquals("Bob", parsed[1].name)
    assertNull(parsed[1].email)
  }

  @Test
  fun `Order CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Order::class)
    val orders = listOf(
      Order(id = 1, accountId = 10, timestamp = 1700000000000L),
      Order(id = 2, accountId = 20, timestamp = 1700000001000L)
    )

    val csv = parser.toCsv(orders)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertEquals(10, parsed[0].accountId)
  }

  @Test
  fun `OrderLine CSV roundtrip`() {
    val parser = parserFactory.getParserForType(OrderLine::class)
    val lines = listOf(
      OrderLine(id = 1,
        orderId = 1,
        itemId = 5,
        itemCount = 2,
        message = "Extra",
        usedVoucher = false,
        paidAmount = 300),
      OrderLine(id = 2,
        orderId = 1,
        itemId = null,
        itemCount = 1,
        message = "Custom",
        usedVoucher = false,
        paidAmount = 150)
    )

    val csv = parser.toCsv(lines)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertEquals(300L, parsed[0].paidAmount)
    assertEquals("Custom", parsed[1].message)
  }

  @Test
  fun `OrderWithOrderLine CSV roundtrip`() {
    val parser = parserFactory.getParserForType(OrderWithOrderLine::class)
    val rows = listOf(
      OrderWithOrderLine(orderId = 1,
        accountId = 10,
        timestamp = 1700000000000L,
        orderLineId = 1,
        itemId = 5,
        itemCount = 2,
        message = "test",
        usedVoucher = false,
        paidAmount = 300)
    )

    val csv = parser.toCsv(rows)
    val parsed = parser.fromCsv(csv)

    assertEquals(1, parsed.size)
    assertEquals(10, parsed[0].accountId)
    assertEquals(300L, parsed[0].paidAmount)
  }

  @Test
  fun `VoucherDto CSV roundtrip`() {
    val parser = parserFactory.getParserForType(OrderAdminController.VoucherDto::class)
    val vouchers = listOf(
      OrderAdminController.VoucherDto(accountId = 1, itemId = 5, count = 3),
      OrderAdminController.VoucherDto(accountId = 2, itemId = 10, count = 1)
    )

    val csv = parser.toCsv(vouchers)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertEquals(1, parsed[0].accountId)
    assertEquals(5, parsed[0].itemId)
    assertEquals(3, parsed[0].count)
  }

  @Test
  fun `Transaction CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Transaction::class)
    val txs = listOf(
      Transaction(id = 1,
        type = TransactionType.CHARGE,
        senderId = 1,
        recipientId = null,
        amount = 500,
        message = "Beer",
        timestamp = 1700000000000L),
      Transaction(id = 2,
        type = TransactionType.TOP_UP,
        senderId = null,
        recipientId = 2,
        amount = 1000,
        message = null,
        timestamp = 1700000001000L)
    )

    val csv = parser.toCsv(txs)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertEquals(TransactionType.CHARGE, parsed[0].type)
    assertEquals(500L, parsed[0].amount)
    assertEquals("Beer", parsed[0].message)
    assertEquals(TransactionType.TOP_UP, parsed[1].type)
  }

  @Test
  fun `Event CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Event::class)
    val events = listOf(
      Event(id = 1, event = "TEST", timestamp = 1700000000000L, message = "Test message", performedBy = "admin")
    )

    val csv = parser.toCsv(events)
    val parsed = parser.fromCsv(csv)

    assertEquals(1, parsed.size)
    assertEquals("TEST", parsed[0].event)
    assertEquals("Test message", parsed[0].message)
  }

  @Test
  fun `Principal CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Principal::class)
    val principals = listOf(
      Principal(id = 1, name = "admin", secret = "hash", role = Role.ADMIN, active = true,
        canUpload = true, canTransfer = true, canSellItems = true,
        canRedeemVouchers = true, canAssignCards = true,
        createdAt = 1700000000000L, lastUsed = 1700000001000L)
    )

    val csv = parser.toCsv(principals)
    val parsed = parser.fromCsv(csv)

    assertEquals(1, parsed.size)
    assertEquals("admin", parsed[0].name)
    assertEquals(Role.ADMIN, parsed[0].role)
  }

  @Test
  fun `PrincipalDto CSV roundtrip`() {
    val parser = parserFactory.getParserForType(PrincipalDto::class)
    val dtos = listOf(
      PrincipalDto(name = "terminal-1", password = "pw", role = Role.TERMINAL,
        canUpload = true, canTransfer = false, canSellItems = true,
        canRedeemVouchers = false, canAssignCards = false, active = true)
    )

    val csv = parser.toCsv(dtos)
    val parsed = parser.fromCsv(csv)

    assertEquals(1, parsed.size)
    assertEquals("terminal-1", parsed[0].name)
    assertEquals(Role.TERMINAL, parsed[0].role)
    assertTrue(parsed[0].canSellItems)
    assertFalse(parsed[0].canRedeemVouchers)
  }

  @Test
  fun `empty CSV roundtrip`() {
    val parser = parserFactory.getParserForType(Account::class)
    val empty = emptyList<Account>()

    val csv = parser.toCsv(empty)
    val parsed = parser.fromCsv(csv)

    assertEquals(0, parsed.size)
  }

  @Test
  fun `CSV with special characters roundtrip`() {
    val parser = parserFactory.getParserForType(Account::class)
    val accounts = listOf(
      Account(id = null,
        name = "User, With Comma",
        email = "a@b.com",
        phone = null,
        card = "C1",
        balance = 100,
        active = true),
      Account(id = null,
        name = "User \"Quoted\"",
        email = null,
        phone = null,
        card = "C2",
        balance = 200,
        active = true)
    )

    val csv = parser.toCsv(accounts)
    val parsed = parser.fromCsv(csv)

    assertEquals(2, parsed.size)
    assertTrue(parsed[0].name.contains(","))
    assertTrue(parsed[1].name.contains("\""))
  }
}
