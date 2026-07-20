package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipal
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class PrincipalAuthenticationSuccessListenerTest {

  private val events: ApplicationEventPublisher = mockk(relaxed = true)
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var listener: PrincipalAuthenticationSuccessListener

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    listener = PrincipalAuthenticationSuccessListener(events, clock)
  }

  @Test
  fun `success event with Principal publishes PrincipalAuthenticatedEvent`() {
    val principal = testPrincipal(id = 1, name = "terminal1", role = Role.TERMINAL)
    val auth = UsernamePasswordAuthenticationToken(principal, "password", principal.authorities)
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify {
      events.publishEvent(match<PrincipalAuthenticatedEvent> {
        it.principal.id == 1 && it.principal.name == "terminal1" && it.timestamp == 1700000000000L
      })
    }
  }

  @Test
  fun `success event with non-Principal does NOT publish event`() {
    val auth = UsernamePasswordAuthenticationToken("just-a-string", "password", emptyList<Nothing>())
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify(exactly = 0) { events.publishEvent(any()) }
  }

  @Test
  fun `success event with UserDetails but not Principal does NOT publish`() {
    val userDetails = org.springframework.security.core.userdetails.User(
      "some-user", "password", true, true, true, true, emptyList()
    )
    val auth = UsernamePasswordAuthenticationToken(userDetails, "password", userDetails.authorities)
    val event = AuthenticationSuccessEvent(auth)

    listener.on(event)

    verify(exactly = 0) { events.publishEvent(any()) }
  }
}
