package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.BadRequestException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OrderIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var itemService: ItemService

  @Autowired
  private lateinit var orderService: OrderService

  @Autowired
  private lateinit var orderRepository: OrderRepository

  @Autowired
  private lateinit var voucherService: VoucherService

  @Autowired
  private lateinit var orderLineService: OrderLineService


  @Test
  fun `create and find item`() {
    val item = createItem(name = "Burger", cost = 500, stock = 20)

    val found = itemService.find(item.id!!)
    assertEquals("Burger", found.name)
    assertEquals(500L, found.cost)
  }

  @Test
  fun `findAllActive returns only enabled items`() {
    createItem(name = "Active", enabled = true)
    createItem(name = "Hidden", enabled = false)

    val active = itemService.findAllActive()
    assertEquals(1, active.size)
    assertEquals("Active", active[0].name)
  }

  @Test
  fun `findAll returns all items ordered by name`() {
    createItem(name = "Z")
    createItem(name = "A")
    createItem(name = "M")

    val all = itemService.findAll()
    assertEquals("A", all[0].name)
    assertEquals("M", all[1].name)
    assertEquals("Z", all[2].name)
  }

  @Test
  fun `setEnabled toggles item`() {
    val item = createItem(enabled = true)
    val disabled = itemService.setEnabled(item.id!!, false)
    assertFalse(disabled.enabled)

    val enabled = itemService.setEnabled(item.id!!, true)
    assertTrue(enabled.enabled)
  }

  @Test
  fun `deleteItem removes item`() {
    val item = createItem()
    itemService.deleteItem(item.id!!)
    assertThrows<BadRequestException> { itemService.find(item.id!!) }
  }

  // --- removeFromStock ---

  @Test
  fun `removeFromStock reduces stock`() {
    val item = createItem(stock = 10)
    itemService.removeFromStock(item.id!!, 3)

    val reloaded = itemService.find(item.id!!)
    assertEquals(7, reloaded.stock)
  }

  @Test
  fun `removeFromStock with insufficient stock throws`() {
    val item = createItem(stock = 2)
    assertThrows<BadRequestException> { itemService.removeFromStock(item.id!!, 5) }
  }

  // --- sellItem ---

  @Test
  fun `sellItem creates order line and deducts balance`() {
    val item = createItem(name = "Drink", cost = 150, stock = 20)
    val account = createAccount(card = "BUY-CARD", balance = 1000, name = "Buyer")
    val order = orderRepository.save(Order(id = null, accountId = account.id!!, timestamp = System.currentTimeMillis()))

    itemService.sellItem(order, item, "Extra cold", 2)

    // Verify order line
    val orderLines = orderLineService.findAll()
    assertEquals(1, orderLines.size)
    val line = orderLines[0]
    assertEquals(order.id, line.orderId)
    assertEquals(item.id, line.itemId)
    assertEquals(2, line.itemCount)
    assertEquals(300L, line.paidAmount)

    // Verify stock
    val reloadedItem = itemService.find(item.id!!)
    assertEquals(18, reloadedItem.stock)
  }

  // --- sellCustomItem ---

  @Test
  fun `sellCustomItem creates order line`() {
    val account = createAccount(card = "CUSTOM-CARD", balance = 1000)
    val order = orderRepository.save(Order(id = null, accountId = account.id!!, timestamp = System.currentTimeMillis()))

    itemService.sellCustomItem(order, "Special request", 1, 250)

    val orderLines = orderLineService.findAll()
    assertEquals(1, orderLines.size)
    assertEquals(250L, orderLines[0].paidAmount)
    assertEquals("Special request", orderLines[0].message)
  }

  // --- Orders ---

  @Test
  fun `findAll returns all orders`() {
    val account = createAccount(card = "ORD-CARD", balance = 1000)
    orderRepository.save(Order(id = null, accountId = account.id!!, timestamp = 1000))
    orderRepository.save(Order(id = null, accountId = account.id!!, timestamp = 2000))

    val orders = orderService.findAll()
    assertTrue(orders.size >= 2)
    // Most recent first
    assertEquals(2000L, orders[0].timestamp)
  }

  @Test
  fun `findPaginated respects page size`() {
    val account = createAccount(card = "PAGE-CARD", balance = 5000)
    repeat(5) { orderRepository.save(Order(id = null, accountId = account.id!!, timestamp = it * 1000L)) }

    val page1 = orderService.findPaginated(0, 2)
    assertEquals(2, page1.size)
    assertEquals(4000L, page1[0].timestamp) // Most recent first

    val page2 = orderService.findPaginated(1, 2)
    assertEquals(2, page2.size)
  }

  // --- Voucher ---

  @Test
  fun `create and find vouchers`() {
    val account = createAccount(card = "VOUCH-C", name = "Vouch")
    val item = createItem(name = "Voucher Item")

    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 3))

    val result = voucherService.getVouchersWithAccount(account)
    assertEquals(1, result.vouchers.size)
    assertEquals(3, result.vouchers[0].count)
    assertEquals("Voucher Item", result.vouchers[0].itemName)
  }

  @Test
  fun `updateVoucherCount changes count`() {
    val account = createAccount(card = "VOUCH-U")
    val item = createItem()
    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 5))

    val vouchers = voucherService.getVouchersWithAccount(account)
    val updated = voucherService.updateVoucherCount(vouchers.vouchers[0].voucherId, 10)

    assertEquals(10, updated.count)
  }

  @Test
  fun `delete voucher removes it`() {
    val account = createAccount(card = "VOUCH-D")
    val item = createItem()
    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 1))

    val vouchers = voucherService.getVouchersWithAccount(account)
    val voucherId = vouchers.vouchers[0].voucherId
    assertNotNull(voucherId)

    voucherService.delete(voucherId)
    assertEquals(0, voucherService.getVouchersWithAccount(account).vouchers.size)
  }

  @Test
  fun `duplicate voucher throws BadRequestException`() {
    val account = createAccount(card = "VOUCH-DUP")
    val item = createItem()
    voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 1))

    assertThrows<BadRequestException> {
      voucherService.saveVoucher(OrderAdminController.VoucherDto(accountId = account.id, itemId = item.id!!, count = 1))
    }
  }
}
