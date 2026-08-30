package hu.bme.sch.kirpay.principal

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
class PrincipalConcurrencyTest {

  @Autowired
  private lateinit var principalService: PrincipalService

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val uniqueName = "concurrent-principal-${System.nanoTime()}"

  @AfterEach
  fun tearDown() {
    principalRepository.findByName(uniqueName)?.let { principalRepository.delete(it) }
  }

  @Test
  fun `concurrent createPrincipal with same name prevents duplicates`() {
    val dto = PrincipalDto(
      name = uniqueName, password = "test-pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )

    val latch = CountDownLatch(1)
    val successCount = AtomicInteger(0)
    val failureCount = AtomicInteger(0)
    val results = mutableListOf<Principal?>()

    val thread1 = Thread {
      latch.await()
      try {
        val result = principalService.createPrincipal(dto, failOnCollision = true)
        results.add(result)
        successCount.incrementAndGet()
      } catch (e: Exception) {
        failureCount.incrementAndGet()
      }
    }

    val thread2 = Thread {
      latch.await()
      try {
        val result = principalService.createPrincipal(dto, failOnCollision = true)
        results.add(result)
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

    assertEquals(1, successCount.get(),
      "Exactly one createPrincipal should succeed. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")

    assertTrue(failureCount.get() >= 1,
      "At least one createPrincipal should fail. Successes: ${successCount.get()}, Failures: ${failureCount.get()}")

    val saved = principalRepository.findByName(uniqueName)
    assertNotNull(saved, "Exactly one principal with the name should exist")
    assertEquals(uniqueName, saved.name)
  }
}
