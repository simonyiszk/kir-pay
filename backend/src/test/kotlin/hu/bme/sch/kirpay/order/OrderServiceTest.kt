package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountService
import hu.bme.sch.kirpay.common.UnprocessableEntityException
import hu.bme.sch.kirpay.common.buildFingerprint
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.testAccount
import hu.bme.sch.kirpay.testOrder
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderServiceTest {

  private val accountService: AccountService = mockk()
  private val orderRepository: OrderRepository = mockk()
  private val voucherService: VoucherService = mockk()
  private val itemService: ItemService = mockk()
  private val eventService: EventService = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var service: OrderService

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    service = OrderService(accountService, orderRepository, voucherService, itemService, eventService, clock)
  }

  private fun checkoutDto(itemId: Int, count: Int = 1, idempotencyKey: UUID? = null) =
    OrderTerminalController.CheckoutDto(
      orderLines = listOf(
        OrderTerminalController.OrderLineDto(
          itemId = itemId, itemCount = count, usedVoucher = false,
          message = null, paidAmount = null
        )
      ),
      idempotencyKey = idempotencyKey ?: UUID.randomUUID()
    )

  private fun fingerprint(card: String, dto: OrderTerminalController.CheckoutDto): String =
    buildFingerprint("CHECKOUT", card, dto.orderLines)

  @Test
  fun `idempotency - first call creates order`() {
    val key = UUID.randomUUID()
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = java.math.BigInteger.valueOf(5000))
    val dto = checkoutDto(itemId = 10, idempotencyKey = key)

    every { accountService.findActiveByCard(card) } returns account
    every { orderRepository.findByIdempotencyKey(key.toString()) } returns null
    every { itemService.processSaleAuthorized(any(), any()) } just Runs

    val savedOrder =
      testOrder(id = 42, accountId = 1, idempotencyKey = key.toString(), requestFingerprint = fingerprint(card, dto))
    every { orderRepository.save(any()) } returns savedOrder

    val result = service.checkout(card, dto)

    assertEquals(42, result.order.id)
    assertEquals(key.toString(), result.order.idempotencyKey)
    assertFalse(result.replayed)
    verify { itemService.processSaleAuthorized(any(), any()) }
  }

  @Test
  fun `idempotency - replay returns existing order without charging again`() {
    val key = UUID.randomUUID()
    val card = "CARD-001"
    val dto = checkoutDto(itemId = 10, idempotencyKey = key)

    val existingOrder =
      testOrder(id = 42, accountId = 1, idempotencyKey = key.toString(), requestFingerprint = fingerprint(card, dto))

    every { orderRepository.findByIdempotencyKey(key.toString()) } returns existingOrder

    val result = service.checkout(card, dto)

    assertEquals(42, result.order.id)
    assertTrue(result.replayed)
    verify(exactly = 0) { itemService.processSaleAuthorized(any(), any()) }
    verify(exactly = 0) { orderRepository.save(any()) }
  }

  @Test
  fun `idempotency - different key creates new order`() {
    val key1 = UUID.randomUUID()
    val key2 = UUID.randomUUID()
    val card = "CARD-001"
    val account = testAccount(id = 1, card = card, balance = java.math.BigInteger.valueOf(5000))
    val dto1 = checkoutDto(itemId = 10, idempotencyKey = key1)
    val dto2 = checkoutDto(itemId = 10, idempotencyKey = key2)

    every { accountService.findActiveByCard(card) } returns account
    every { orderRepository.findByIdempotencyKey(key1.toString()) } returns null
    every { orderRepository.findByIdempotencyKey(key2.toString()) } returns null
    every { itemService.processSaleAuthorized(any(), any()) } just Runs

    val savedOrder1 =
      testOrder(id = 42, accountId = 1, idempotencyKey = key1.toString(), requestFingerprint = fingerprint(card, dto1))
    val savedOrder2 =
      testOrder(id = 43, accountId = 1, idempotencyKey = key2.toString(), requestFingerprint = fingerprint(card, dto2))
    every { orderRepository.save(any<Order>()) } returns savedOrder1 andThen savedOrder2

    val result1 = service.checkout(card, dto1)
    val result2 = service.checkout(card, dto2)

    assertEquals(42, result1.order.id)
    assertEquals(43, result2.order.id)
    assertEquals(key1.toString(), result1.order.idempotencyKey)
    assertEquals(key2.toString(), result2.order.idempotencyKey)
    assertFalse(result1.replayed)
    assertFalse(result2.replayed)
    verify(exactly = 2) { itemService.processSaleAuthorized(any(), any()) }
  }

  @Test
  fun `idempotency - different payload with same key throws 422`() {
    val key = UUID.randomUUID()
    val card = "CARD-001"
    val dto1 = checkoutDto(itemId = 10, idempotencyKey = key)
    val dto2 = checkoutDto(itemId = 20, count = 3, idempotencyKey = key)

    val existingOrder =
      testOrder(id = 42, accountId = 1, idempotencyKey = key.toString(), requestFingerprint = fingerprint(card, dto1))

    every { orderRepository.findByIdempotencyKey(key.toString()) } returns existingOrder

    assertThrows<UnprocessableEntityException> {
      service.checkout(card, dto2)
    }
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
