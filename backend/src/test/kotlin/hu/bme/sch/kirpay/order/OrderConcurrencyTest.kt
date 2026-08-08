package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.account.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyTest {

  @Autowired
  private lateinit var voucherService: VoucherService

  @Autowired
  private lateinit var itemService: ItemService

  @Autowired
  private lateinit var itemRepository: ItemRepository

  @Autowired
  private lateinit var voucherRepository: VoucherRepository

  @Autowired
  private lateinit var accountRepository: AccountRepository

  private val uniqueSuffix = System.nanoTime()

  @BeforeEach
  fun setUp() {
    cleanup()
  }

  @AfterEach
  fun tearDown() {
    cleanup()
  }

  private fun cleanup() {
    voucherRepository.findAllOrderByAccountId().forEach { voucherRepository.delete(it) }
    itemRepository.findByEnabledOrderByName(true).filter { it.name.contains("CONC-") }
      .forEach { itemRepository.delete(it) }
    itemRepository.findAllOrderByName().filter { it.name.contains("CONC-") }.forEach { itemRepository.delete(it) }
    accountRepository.findByCard("CONC-CARD-$uniqueSuffix")?.let { accountRepository.delete(it) }
  }

  @Test
  fun `concurrent incrementCount prevents negative voucher count`() {

    val account = accountRepository.save(
      hu.bme.sch.kirpay.account.Account(
        id = null, name = "Concurrent Voucher User", email = null, phone = null,
        card = "CONC-CARD-$uniqueSuffix", balance = java.math.BigInteger.valueOf(0), active = true
      )
    )
    val item = itemRepository.save(
      Item(id = null,
        name = "CONC-Voucher-Item",
        alias = null,
        cost = java.math.BigInteger.valueOf(0),
        stock = 100,
        enabled = true,
        showOnLeaderboard = false)
    )
    val voucher = voucherRepository.save(
      Voucher(id = null, accountId = account.id, itemId = item.id!!, count = 5)
    )
    val voucherId = voucher.id!!

    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)
    val failureCount = AtomicInteger(0)

    val thread1 = Thread {
      latch.await()
      try {
        voucherService.incrementCount(voucherId, -3, UUID.randomUUID())
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    val thread2 = Thread {
      latch.await()
      try {
        voucherService.incrementCount(voucherId, -3, UUID.randomUUID())
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    thread1.start()
    thread2.start()
    latch.countDown()

    thread1.join(TimeUnit.SECONDS.toMillis(15))
    thread2.join(TimeUnit.SECONDS.toMillis(15))

    assertEquals(1,
      successCount.get(),
      "Exactly one decrement should succeed. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")
    assertTrue(failureCount.get() >= 1,
      "At least one decrement should fail. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")

    val reloaded = voucherRepository.findById(voucherId).get()
    assertTrue(reloaded.count >= 0, "Voucher count must never be negative, got ${reloaded.count}")
    assertEquals(2, reloaded.count, "Final voucher count should be 5-3=2")
  }

  @Test
  fun `concurrent removeFromStock prevents overselling`() {

    val item = itemRepository.save(
      Item(id = null,
        name = "CONC-Stock-Item",
        alias = null,
        cost = java.math.BigInteger.valueOf(100),
        stock = 2,
        enabled = true,
        showOnLeaderboard = false)
    )
    val itemId = item.id!!

    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)
    val failureCount = AtomicInteger(0)

    val thread1 = Thread {
      latch.await()
      try {
        itemService.removeFromStock(itemId, 2)
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    val thread2 = Thread {
      latch.await()
      try {
        itemService.removeFromStock(itemId, 2)
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    thread1.start()
    thread2.start()
    latch.countDown()

    thread1.join(TimeUnit.SECONDS.toMillis(15))
    thread2.join(TimeUnit.SECONDS.toMillis(15))

    assertEquals(1,
      successCount.get(),
      "Exactly one removeFromStock should succeed. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")
    assertTrue(failureCount.get() >= 1,
      "At least one removeFromStock should fail. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")

    val reloaded = itemRepository.findById(itemId).get()
    assertTrue(reloaded.stock >= 0, "Stock must never be negative, got ${reloaded.stock}")
    assertEquals(0, reloaded.stock, "Final stock should be 2-2=0")
  }
}
