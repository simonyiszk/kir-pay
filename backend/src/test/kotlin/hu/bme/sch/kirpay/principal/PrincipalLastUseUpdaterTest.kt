package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipal
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PrincipalLastUseUpdaterTest {

  private val principalService: PrincipalService = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var updater: PrincipalLastUseUpdater

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    updater = PrincipalLastUseUpdater(principalService, clock)
  }

  @Test
  fun `updateIfNeeded with old lastUsed calls updateLastUsed`() {
    val principal = testPrincipal(id = 42, name = "terminal1", lastUsed = 1699999940000L)
    every { principalService.find(42) } returns principal

    updater.updateIfNeeded(PrincipalRef(42, "terminal1"), 1700000000000L)

    verify { principalService.find(42) }
    verify { principalService.updateLastUsed(42) }
  }

  @Test
  fun `updateIfNeeded with recent lastUsed skips update`() {
    val principal = testPrincipal(id = 42, name = "terminal1", lastUsed = 1700000000000L)
    every { principalService.find(42) } returns principal

    updater.updateIfNeeded(PrincipalRef(42, "terminal1"), 1700000005000L)

    verify { principalService.find(42) }
    verify(exactly = 0) { principalService.updateLastUsed(any()) }
  }

  @Test
  fun `updateIfNeeded with null id does NOT crash`() {
    updater.updateIfNeeded(PrincipalRef(null, "unknown"), 1700000000000L)

    verify(exactly = 0) { principalService.updateLastUsed(any()) }
    verify(exactly = 0) { principalService.find(any()) }
  }
}
