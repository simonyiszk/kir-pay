package hu.bme.sch.kirpay.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class IdempotentOperationCleanupTest {

  private val idempotentOperationRepository: IdempotentOperationRepository = mockk()
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))
  private val cleanup = IdempotentOperationCleanup(idempotentOperationRepository, clock)

  @Test
  fun `cleanup deletes entries older than 7 days`() {
    val cutoff = 1700000000000L - 7 * 24 * 60 * 60 * 1000L
    every { idempotentOperationRepository.deleteByCreatedAtBefore(cutoff) } returns 5

    cleanup.cleanup()

    verify(exactly = 1) { idempotentOperationRepository.deleteByCreatedAtBefore(cutoff) }
  }
}
