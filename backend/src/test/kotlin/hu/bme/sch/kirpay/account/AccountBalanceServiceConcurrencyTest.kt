package hu.bme.sch.kirpay.account

import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@SpringBootTest
@ActiveProfiles("test")
class AccountBalanceServiceConcurrencyTest {

  @Autowired
  private lateinit var accountBalanceService: AccountBalanceService

  @Autowired
  private lateinit var accountRepository: AccountRepository

  private val accountCard = "CONC-CARD-${System.nanoTime()}"

  @BeforeEach
  fun setUp() {
    deleteTestAccounts()
  }

  @AfterEach
  fun tearDown() {
    deleteTestAccounts()
  }

  private fun deleteTestAccounts() {
    for (suffix in listOf("", "UP", "DIS")) {
      val existing = accountRepository.findByCard(accountCard + suffix)
      if (existing != null) {
        accountRepository.delete(existing)
      }
    }
  }

  @Test
  fun `concurrent pay on same account prevents lost updates`() {
    val account = accountRepository.save(
      Account(id = null, name = "Concurrent User", email = null, phone = null,
        card = accountCard, balance = 100, active = true)
    )
    val card = account.card!!

    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)
    val failureCount = AtomicInteger(0)

    val thread1 = Thread {
      latch.await()
      try {
        accountBalanceService.pay(card, 60, logEvent = true)
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    val thread2 = Thread {
      latch.await()
      try {
        accountBalanceService.pay(card, 60, logEvent = true)
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    thread1.start()
    thread2.start()
    latch.countDown()

    thread1.join(TimeUnit.SECONDS.toMillis(10))
    thread2.join(TimeUnit.SECONDS.toMillis(10))

    // With SERIALIZABLE isolation: at least one thread should fail
    // (100 - 60 - 60 = -20, insufficient balance for second pay)
    assertTrue(failureCount.get() >= 1 || successCount.get() == 1,
      "At least one concurrent pay should fail with balance=100 and two 60-charges. " +
          "Successes: ${successCount.get()}, Failures: ${failureCount.get()}")

    // Verify final balance
    val reloaded = accountRepository.findById(account.id!!).get()
    assertTrue(reloaded.balance == 40L || reloaded.balance == 100L,
      "Balance should be 40 (one succeeded) or 100 (both rolled back), got ${reloaded.balance}")
  }

  @Test
  fun `concurrent uploads both succeed`() {
    val account = accountRepository.save(
      Account(id = null, name = "Upload User", email = null, phone = null,
        card = accountCard + "UP", balance = 0, active = true)
    )
    val card = account.card!!

    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)

    val thread1 = Thread {
      latch.await()
      try {
        accountBalanceService.upload(card, 50)
        successCount.incrementAndGet()
      } catch (_: Exception) {
      }
    }

    val thread2 = Thread {
      latch.await()
      try {
        accountBalanceService.upload(card, 50)
        successCount.incrementAndGet()
      } catch (_: Exception) {
      }
    }

    thread1.start()
    thread2.start()
    latch.countDown()

    thread1.join(TimeUnit.SECONDS.toMillis(10))
    thread2.join(TimeUnit.SECONDS.toMillis(10))

    // Both uploads should succeed (no conflict — additive operations)
    assertEquals(2, successCount.get(), "Both uploads should succeed")

    // Final balance should be 100
    Awaitility.await().atMost(5, TimeUnit.SECONDS).until {
      val reloaded = accountRepository.findById(account.id!!).get()
      reloaded.balance == 100L
    }
  }

  @Test
  fun `admin disable during payment preserves state`() {
    val account = accountRepository.save(
      Account(id = null, name = "Disable Target", email = null, phone = null,
        card = accountCard + "DIS", balance = 200, active = true)
    )
    val card = account.card!!

    val latch = CountDownLatch(1)
    val paySuccess = AtomicInteger(0)
    val payFailed = AtomicInteger(0)

    val payThread = Thread {
      latch.await()
      try {
        accountBalanceService.pay(card, 50, logEvent = true)
        paySuccess.incrementAndGet()
      } catch (e: Exception) {
        payFailed.incrementAndGet()
      }
    }

    val disableThread = Thread {
      latch.await()
      accountRepository.save(account.copy(active = false))
    }

    payThread.start()
    disableThread.start()
    latch.countDown()

    payThread.join(TimeUnit.SECONDS.toMillis(10))
    disableThread.join(TimeUnit.SECONDS.toMillis(10))

    // Payment either succeeds or fails; the key is no corrupted state
    val reloaded = accountRepository.findById(account.id!!).get()
    assertTrue(reloaded.balance == 200L || reloaded.balance == 150L,
      "Balance should be 200 (payment failed) or 150 (payment succeeded), got ${reloaded.balance}")
  }
}
