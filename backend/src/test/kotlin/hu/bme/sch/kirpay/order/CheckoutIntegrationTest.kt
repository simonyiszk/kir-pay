package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.common.NotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class CheckoutIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var orderService: OrderService

  @Autowired
  private lateinit var itemService: ItemService

  @Autowired
  private lateinit var voucherService: VoucherService

  @Autowired
  private lateinit var orderLineService: OrderLineService

  @Test
  fun `checkout with single item deducts balance and stock`() {
    val account = createAccount(card = "CHK-CARD1", balance = 2000, name = "Shopper")
    val item = createItem(name = "Soda", cost = 150, stock = 30)

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = item.id, itemCount = 3, usedVoucher = false,
          message = null, paidAmount = null
        )
      )
    )

    orderService.checkout("CHK-CARD1", dto)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(2000 - 450, reloaded.balance, "Balance should be debited 3×150=450")

    val reloadedItem = itemRepository.findById(item.id!!).get()
    assertEquals(27, reloadedItem.stock, "Stock should be reduced by 3")

    val orders = orderService.findAll()
    assertTrue(orders.isNotEmpty())

    val orderLines = orderLineService.findAll()
    assertTrue(orderLines.isNotEmpty())
    val line = orderLines.first()
    assertEquals(3, line.itemCount)
    assertEquals(450L, line.paidAmount)
  }

  @Test
  fun `checkout with custom item deducts balance`() {
    val account = createAccount(card = "CHK-CUSTOM", balance = 1000, name = "CustomBuyer")

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = null, itemCount = 2, usedVoucher = false,
          message = "Special request", paidAmount = 250
        )
      )
    )

    orderService.checkout("CHK-CUSTOM", dto)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(500, reloaded.balance, "Balance should be debited 2×250=500")

    val orderLines = orderLineService.findAll()
    val line = orderLines.first()
    assertEquals(500L, line.paidAmount)
    assertEquals("Special request", line.message)
  }

  @Test
  fun `checkout with insufficient balance throws`() {
    val account = createAccount(card = "CHK-POOR", balance = 100, name = "Poor")
    val item = createItem(name = "Expensive", cost = 500, stock = 10)

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = item.id, itemCount = 1, usedVoucher = false,
          message = null, paidAmount = null
        )
      )
    )

    assertThrows<BadRequestException> {
      orderService.checkout("CHK-POOR", dto)
    }

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(100, reloaded.balance)
  }

  @Test
  fun `checkout with insufficient stock throws`() {
    val account = createAccount(card = "CHK-STOCK", balance = 5000, name = "Rich")
    val item = createItem(name = "Rare Item", cost = 100, stock = 2)

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = item.id, itemCount = 5, usedVoucher = false,
          message = null, paidAmount = null
        )
      )
    )

    assertThrows<BadRequestException> {
      orderService.checkout("CHK-STOCK", dto)
    }
  }

  @Test
  fun `checkout with multiple lines processes all`() {
    val account = createAccount(card = "CHK-MULTI", balance = 3000, name = "MultiBuyer")
    val item1 = createItem(name = "Burger", cost = 300, stock = 20)
    val item2 = createItem(name = "Fries", cost = 150, stock = 30)

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(itemId = item1.id,
          itemCount = 2,
          usedVoucher = false,
          message = null,
          paidAmount = null),
        OrderTerminalController.OrderLineDto(itemId = item2.id,
          itemCount = 3,
          usedVoucher = false,
          message = null,
          paidAmount = null),
        OrderTerminalController.OrderLineDto(itemId = null,
          itemCount = 1,
          usedVoucher = false,
          message = "Tip",
          paidAmount = 100)
      )
    )

    orderService.checkout("CHK-MULTI", dto)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(1850, reloaded.balance)

    val orderLines = orderLineService.findAll()
    assertEquals(3, orderLines.size)
  }

  @Test
  fun `checkout with voucher does not charge account`() {
    val account = createAccount(card = "CHK-VOUCH", balance = 1000, name = "VouchUser")
    val item = createItem(name = "Free Gift", cost = 500, stock = 10)

    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 2))

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = item.id, itemCount = 1, usedVoucher = true,
          message = null, paidAmount = null
        )
      )
    )

    orderService.checkout("CHK-VOUCH", dto)

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(1000, reloaded.balance, "Voucher should not charge account")

    val vouchers = voucherService.getVouchersWithAccount(account)
    assertEquals(1, vouchers.vouchers[0].count, "Voucher count should decrease by 1")

    val reloadedItem = itemRepository.findById(item.id!!).get()
    assertEquals(9, reloadedItem.stock)
  }

  @Test
  fun `checkout with unknown card throws`() {
    val item = createItem(name = "Item", cost = 100, stock = 10)

    val dto = OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(itemId = item.id,
          itemCount = 1,
          usedVoucher = false,
          message = null,
          paidAmount = null)
      )
    )

    assertThrows<NotFoundException> {
      orderService.checkout("NOBODY", dto)
    }
  }

  @Test
  fun `checkout respects stock across multiple orders`() {
    val account = createAccount(card = "CHK-STOCK2", balance = 5000, name = "StockUser")
    val item = createItem(name = "Limited", cost = 100, stock = 5)

    orderService.checkout("CHK-STOCK2", OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(itemId = item.id,
          itemCount = 3,
          usedVoucher = false,
          message = null,
          paidAmount = null)
      )
    ))

    assertThrows<BadRequestException> {
      orderService.checkout("CHK-STOCK2", OrderTerminalController.CheckoutDto(
        orderLines = listOf(
          OrderTerminalController.OrderLineDto(itemId = item.id,
            itemCount = 3,
            usedVoucher = false,
            message = null,
            paidAmount = null)
        )
      ))
    }

    val reloaded = accountRepository.findById(account.id!!).get()
    assertEquals(4700, reloaded.balance, "Only first order's charge should apply")
  }
}
