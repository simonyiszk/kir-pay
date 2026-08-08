package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipal
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PrincipalAuthenticationSuccessListenerTest {

  private val principalLastUseUpdater: PrincipalLastUseUpdater = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var listener: PrincipalAuthenticationSuccessListener

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    listener = PrincipalAuthenticationSuccessListener(principalLastUseUpdater, clock)
  }

  @Test
  fun `success event with Principal calls updateIfNeeded`() {
    val principal = testPrincipal(id = 1, name = "terminal1", role = Role.TERMINAL)
    val auth = UsernamePasswordAuthenticationToken(principal, "password", principal.authorities)
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify {
      principalLastUseUpdater.updateIfNeeded(match<PrincipalRef> {
        it.id == 1 && it.name == "terminal1"
      }, 1700000000000L)
    }
  }

  @Test
  fun `success event with non-Principal does NOT call updateIfNeeded`() {
    val auth = UsernamePasswordAuthenticationToken("just-a-string", "password", emptyList<Nothing>())
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify(exactly = 0) { principalLastUseUpdater.updateIfNeeded(any(), any()) }
  }

  @Test
  fun `success event with UserDetails but not Principal does NOT call updateIfNeeded`() {
    val userDetails = org.springframework.security.core.userdetails.User(
      "some-user", "password", true, true, true, true, emptyList()
    )
    val auth = UsernamePasswordAuthenticationToken(userDetails, "password", userDetails.authorities)
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify(exactly = 0) { principalLastUseUpdater.updateIfNeeded(any(), any()) }
  }
}
