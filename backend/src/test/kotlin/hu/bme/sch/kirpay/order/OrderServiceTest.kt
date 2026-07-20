package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountService
import hu.bme.sch.kirpay.testAccount
import hu.bme.sch.kirpay.testOrder
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull


class OrderServiceTest {

  private val accountService: AccountService = mockk()
  private val orderRepository: OrderRepository = mockk()
  private val voucherService: VoucherService = mockk()
  private val itemService: ItemService = mockk()
  private val events: ApplicationEventPublisher = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: OrderService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = OrderService(accountService, orderRepository, voucherService, itemService, events, clock)
  }

  private fun checkoutDto(itemId: Int, count: Int = 1, idempotencyKey: UUID? = null) =
    OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = itemId, itemCount = count, usedVoucher = false,
          message = null, paidAmount = null
        )
      ),
      idempotencyKey = idempotencyKey
    )

  @Test
  fun `idempotency — first call creates order`() {
    val key = UUID.randomUUID()
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 5000)

    every { accountService.findActiveByCard(card) } returns account
    every { orderRepository.findByIdempotencyKey(key.toString()) } returns null
    every { itemService.processSaleAuthorized(any(), any()) } just Runs

    val savedOrder = testOrder(id = 42, accountId = 1, idempotencyKey = key.toString())
    every { orderRepository.save(any()) } returns savedOrder

    val result = service.checkout(card, checkoutDto(itemId = 10, idempotencyKey = key))

    assertEquals(42, result.id)
    assertEquals(key.toString(), result.idempotencyKey)
    verify { itemService.processSaleAuthorized(any(), any()) }
  }

  @Test
  fun `idempotency — replay returns existing order without charging again`() {
    val key = UUID.randomUUID()
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 5000)

    val existingOrder = testOrder(id = 42, accountId = 1, idempotencyKey = key.toString())

    every { accountService.findActiveByCard(card) } returns account
    every { orderRepository.findByIdempotencyKey(key.toString()) } returns existingOrder

    val result = service.checkout(card, checkoutDto(itemId = 10, idempotencyKey = key))

    assertEquals(42, result.id)
    verify(exactly = 0) { itemService.processSaleAuthorized(any(), any()) }
    verify(exactly = 0) { orderRepository.save(any()) }
  }

  @Test
  fun `idempotency — different key creates new order`() {
    val key1 = UUID.randomUUID()
    val key2 = UUID.randomUUID()
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 5000)

    every { accountService.findActiveByCard(card) } returns account
    every { orderRepository.findByIdempotencyKey(key1.toString()) } returns null
    every { orderRepository.findByIdempotencyKey(key2.toString()) } returns null
    every { itemService.processSaleAuthorized(any(), any()) } just Runs

    val savedOrder1 = testOrder(id = 42, accountId = 1, idempotencyKey = key1.toString())
    val savedOrder2 = testOrder(id = 43, accountId = 1, idempotencyKey = key2.toString())
    every { orderRepository.save(any<Order>()) } returns savedOrder1 andThen savedOrder2

    val result1 = service.checkout(card, checkoutDto(itemId = 10, idempotencyKey = key1))
    val result2 = service.checkout(card, checkoutDto(itemId = 10, idempotencyKey = key2))

    assertEquals(42, result1.id)
    assertEquals(43, result2.id)
    assertEquals(key1.toString(), result1.idempotencyKey)
    assertEquals(key2.toString(), result2.idempotencyKey)
    verify(exactly = 2) { itemService.processSaleAuthorized(any(), any()) }
  }

  @Test
  fun `no idempotency key — backward compatible`() {
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = 5000)

    every { accountService.findActiveByCard(card) } returns account
    every { itemService.processSaleAuthorized(any(), any()) } just Runs

    val savedOrder = testOrder(id = 42, accountId = 1, idempotencyKey = null)
    every { orderRepository.save(any()) } returns savedOrder

    val result = service.checkout(card, checkoutDto(itemId = 10, idempotencyKey = null))

    assertEquals(42, result.id)
    assertNull(result.idempotencyKey)
    verify { itemService.processSaleAuthorized(any(), any()) }
    verify { orderRepository.save(any()) }
  }

  @Test
  fun `findPaginated delegates to repository with correct offset`() {
    val orders = listOf(testOrder(id = 1), testOrder(id = 2))
    every { orderRepository.findAllOrderByTimestampDescPaginated(10, 5) } returns orders

    val result = service.findPaginated(2, 5)

    assertEquals(2, result.size)
    verify { orderRepository.findAllOrderByTimestampDescPaginated(10, 5) }
  }

  @Test
  fun `findAll returns all orders`() {
    val orders = listOf(testOrder(id = 1), testOrder(id = 2))
    every { orderRepository.findAllOrderByTimestampDesc() } returns orders

    val result = service.findAll()

    assertEquals(2, result.size)
  }
}
