package hu.bme.sch.kirpay.principal

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class PrincipalLastUseUpdaterTest {

  private val principalService: PrincipalService = mockk(relaxed = true)

  private lateinit var updater: PrincipalLastUseUpdater

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    updater = PrincipalLastUseUpdater(principalService)
  }

  @Test
  fun `on PrincipalAuthenticatedEvent calls updateLastUsed`() {
    val event = PrincipalAuthenticatedEvent(PrincipalRef(42, "terminal1"), 1700000000000L)

    updater.on(event)

    verify { principalService.updateLastUsed(42) }
  }

  @Test
  fun `on event with null id does NOT crash`() {
    val event = PrincipalAuthenticatedEvent(PrincipalRef(null, "unknown"), 1700000000000L)

    updater.on(event)

    verify { principalService.updateLastUsed(null) }
  }
}
