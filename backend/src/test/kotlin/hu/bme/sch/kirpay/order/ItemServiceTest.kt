package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountBalanceService
import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.testAccount
import hu.bme.sch.kirpay.testItem
import hu.bme.sch.kirpay.testOrder
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

/**
 * Unit tests for ItemService.
 * Tests BUG-04, BUG-05 related scenarios.
 */
class ItemServiceTest {

  private val accountBalanceService: AccountBalanceService = mockk()
  private val itemRepository: ItemRepository = mockk()
  private val orderLineService: OrderLineService = mockk()
  private val events: ApplicationEventPublisher = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: ItemService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = ItemService(accountBalanceService, itemRepository, orderLineService, events, clock)
  }

  // --- find ---

  @Test
  fun `find existing item returns it`() {
    val item = testItem(id = 1)
    every { itemRepository.findById(1) } returns Optional.of(item)

    val result = service.find(1)

    assertEquals(1, result.id)
    assertEquals("Test Item", result.name)
  }

  @Test
  fun `find non-existent item throws BadRequestException`() {
    every { itemRepository.findById(999) } returns Optional.empty()

    assertThrows<BadRequestException> { service.find(999) }
  }

  // --- findActiveItem ---

  @Test
  fun `findActiveItem returns enabled item`() {
    val item = testItem(id = 1, enabled = true)
    every { itemRepository.findById(1) } returns Optional.of(item)

    val result = service.findActiveItem(1)

    assertEquals(1, result.id)
  }

  @Test
  fun `findActiveItem throws for disabled item`() {
    val item = testItem(id = 1, enabled = false)
    every { itemRepository.findById(1) } returns Optional.of(item)

    assertThrows<BadRequestException> { service.findActiveItem(1) }
  }

  // --- sellItem ---

  @Test
  fun `sellItem deducts stock and balance`() {
    val item = testItem(id = 1, cost = 100, stock = 10)
    val order = testOrder(id = 5, accountId = 1)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.save(any<Item>()) } answers { firstArg() }
    every { accountBalanceService.pay(any<Int>(), any<Long>(), any<Boolean>()) } returns testAccount()
    every { orderLineService.save(any<OrderLine>()) } answers { firstArg() }

    service.sellItem(order, item, "Test message", 3)

    verify { itemRepository.save(match { it.stock == 7 }) }
    verify { accountBalanceService.pay(order.accountId, 300, logEvent = false) }
    verify { events.publishEvent(any<ItemSoldEvent>()) }
  }

  @Test
  fun `sellItem with large cost and count should prevent overflow`() {
    val item = testItem(id = 1, cost = 4611686018427387904L, stock = 100) // 2^62
    val order = testOrder(id = 5, accountId = 1)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.save(any<Item>()) } answers { firstArg() }
    every { accountBalanceService.pay(any<Int>(), any<Long>(), any<Boolean>()) } returns testAccount()
    every { orderLineService.save(any<OrderLine>()) } answers { firstArg() }

    assertThrows<BadRequestException> { service.sellItem(order, item, null, 2) }
  }

  @Test
  fun `sellItem with zero cost should either skip transaction or be rejected`() {
    val item = testItem(id = 1, cost = 0, stock = 10)
    val order = testOrder(id = 5, accountId = 1)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.save(any<Item>()) } answers { firstArg() }
    every { orderLineService.save(any<OrderLine>()) } answers { firstArg() }

    service.sellItem(order, item, null, 1)

    // pay should NOT be called because amount=0
    verify(exactly = 0) { accountBalanceService.pay(any<Int>(), any<Long>(), any<Boolean>()) }
    // order line still created
    verify { orderLineService.save(match { it.paidAmount == 0L && it.itemCount == 1 }) }
  }

  // --- sellCustomItem ---

  @Test
  fun `sellCustomItem charges account and saves order line`() {
    val order = testOrder(id = 5, accountId = 1)
    every { accountBalanceService.pay(any<Int>(), any<Long>(), any<Boolean>()) } returns testAccount()
    every { orderLineService.save(any<OrderLine>()) } answers { firstArg() }

    service.sellCustomItem(order, "Custom item", 2, 150)

    verify { accountBalanceService.pay(order.accountId, 300, logEvent = false) }
    verify { events.publishEvent(any<ItemSoldEvent>()) }
  }

  @Test
  fun `sellCustomItem with overflow should throw`() {
    val order = testOrder(id = 5, accountId = 1)
    assertThrows<BadRequestException> {
      service.sellCustomItem(order, "Overflow", 4, 4611686018427387904L)
    }
  }

  // --- removeFromStock ---

  @Test
  fun `removeFromStock reduces stock`() {
    val item = testItem(id = 1, stock = 10)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.save(any<Item>()) } answers { firstArg() }

    service.removeFromStock(1, 3)

    verify { itemRepository.save(match<Item> { it.stock == 7 }) }
  }

  @Test
  fun `removeFromStock with insufficient stock throws BadRequestException`() {
    val item = testItem(id = 1, stock = 2)
    every { itemRepository.findById(1) } returns Optional.of(item)

    assertThrows<BadRequestException> { service.removeFromStock(1, 5) }
  }

  // --- createItem / updateItem / deleteItem / setEnabled ---

  @Test
  fun `createItem saves and publishes event`() {
    val item = testItem(id = null)
    every { itemRepository.save(any()) } returns testItem(id = 5)

    val result = service.createItem(item)

    assertEquals(5, result.id)
    verify { events.publishEvent(any<ItemCreatedEvent>()) }
  }

  @Test
  fun `deleteItem deletes and publishes event`() {
    val item = testItem(id = 1)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.deleteById(1) } just Runs

    service.deleteItem(1)

    verify { itemRepository.deleteById(1) }
    verify { events.publishEvent(any<ItemDeletedEvent>()) }
  }

  @Test
  fun `setEnabled toggles enabled flag`() {
    val item = testItem(id = 1, enabled = true)
    every { itemRepository.findById(1) } returns Optional.of(item)
    every { itemRepository.save(any()) } answers { firstArg() }

    val result = service.setEnabled(1, false)

    assertFalse(result.enabled)
  }
}
