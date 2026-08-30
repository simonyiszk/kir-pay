package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.testPrincipal
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PrincipalAuthenticationDetailsManagerTest {

  private val principalRepository: PrincipalRepository = mockk()
  private val eventService: EventService = mockk(relaxed = true)
  private val passwordEncoder: PasswordEncoder = mockk()
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1700000000000L), ZoneId.of("UTC"))

  private lateinit var manager: PrincipalAuthenticationDetailsManager

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    every { passwordEncoder.encode(any()) } answers { "enc:" + firstArg<String>() }
    manager = PrincipalAuthenticationDetailsManager(principalRepository, eventService, clock, passwordEncoder)
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `loadUserByUsername returns Principal when found`() {
    val principal = testPrincipal(id = 1, name = "terminal1", role = Role.TERMINAL)
    every { principalRepository.findByName("terminal1") } returns principal

    val result = manager.loadUserByUsername("terminal1")

    assertNotNull(result)
    assertEquals("terminal1", result.username)
    assertTrue(result is Principal)
  }

  @Test
  fun `loadUserByUsername throws UsernameNotFoundException when not found`() {
    every { principalRepository.findByName("unknown") } returns null

    assertThrows<UsernameNotFoundException> {
      manager.loadUserByUsername("unknown")
    }
  }

  @Test
  fun `loadUserByUsername does NOT publish authenticated event`() {
    val principal = testPrincipal(id = 1, name = "terminal1", role = Role.TERMINAL)
    every { principalRepository.findByName("terminal1") } returns principal

    manager.loadUserByUsername("terminal1")

    verify(exactly = 0) { eventService.logPrincipalCreated(any(), any(), any()) }
    verify(exactly = 0) { eventService.logPrincipalUpdated(any(), any(), any()) }
    verify(exactly = 0) { eventService.logPrincipalDeleted(any(), any(), any()) }
  }

  @Test
  fun `createUser saves principal and publishes event`() {
    val user = User("new-terminal", "encoded-password", true, true, true, true,
      listOf(SimpleGrantedAuthority("ROLE_TERMINAL"), SimpleGrantedAuthority("SELL_ITEMS")))
    val savedPrincipal = testPrincipal(id = 5, name = "new-terminal")
    every { principalRepository.save(any()) } returns savedPrincipal

    manager.createUser(user)

    verify { principalRepository.save(match { it.name == "new-terminal" && it.secret == "enc:encoded-password" }) }
    verify { passwordEncoder.encode("encoded-password") }
    verify { eventService.logPrincipalCreated(any(), any(), any()) }
  }

  @Test
  fun `createUser with valid user succeeds`() {
    val user = User("valid-user", "pw", true, true, true, true,
      listOf(SimpleGrantedAuthority("ROLE_TERMINAL"), SimpleGrantedAuthority("SELL_ITEMS")))
    val savedPrincipal = testPrincipal(id = 10, name = "valid-user")
    every { principalRepository.save(any()) } returns savedPrincipal

    manager.createUser(user)

    verify { principalRepository.save(match { it.name == "valid-user" && it.secret == "enc:pw" }) }
    verify { passwordEncoder.encode("pw") }
    verify { eventService.logPrincipalCreated(any(), any(), any()) }
  }

  @Test
  fun `updateUser updates principal and publishes event`() {
    val existing = testPrincipal(id = 1, name = "old-terminal", role = Role.TERMINAL)
    val user = User("old-terminal", "new-password", false, true, true, true,
      listOf(SimpleGrantedAuthority("ROLE_TERMINAL"), SimpleGrantedAuthority("SELL_ITEMS")))

    every { principalRepository.findByName("old-terminal") } returns existing
    every { principalRepository.save(any()) } answers { firstArg() }

    manager.updateUser(user)

    verify { principalRepository.save(match { it.name == "old-terminal" && it.secret == "enc:new-password" && !it.active }) }
    verify { eventService.logPrincipalUpdated(any(), any(), any()) }
  }

  @Test
  fun `updateUser with unknown name throws UsernameNotFoundException`() {
    val user = User("unknown", "pw", true, true, true, true, emptyList())
    every { principalRepository.findByName("unknown") } returns null

    assertThrows<UsernameNotFoundException> {
      manager.updateUser(user)
    }
  }

  @Test
  fun `deleteUser deletes principal and publishes event`() {
    val principal = testPrincipal(id = 1, name = "to-delete")
    every { principalRepository.findByName("to-delete") } returns principal
    every { principalRepository.delete(principal) } just Runs

    manager.deleteUser("to-delete")

    verify { principalRepository.delete(principal) }
    verify { eventService.logPrincipalDeleted(any(), any(), any()) }
  }

  @Test
  fun `deleteUser with unknown name throws IllegalArgumentException`() {
    every { principalRepository.findByName("unknown") } returns null

    assertThrows<IllegalArgumentException> {
      manager.deleteUser("unknown")
    }
  }

  @Test
  fun `changePassword updates secret`() {
    val principal = testPrincipal(id = 1, name = "user1", secret = "old-hash")
    every { principalRepository.findByName("user1") } returns principal
    every { principalRepository.save(any()) } answers { firstArg() }

    val auth = UsernamePasswordAuthenticationToken(principal, "old-pw", principal.authorities)
    SecurityContextHolder.getContext().authentication = auth

    manager.changePassword("old-pw", "new-pw")

    verify { principalRepository.save(match { it.secret == "enc:new-pw" }) }
  }

  @Test
  fun `changePassword with no auth context throws AccessDeniedException`() {
    SecurityContextHolder.clearContext()

    assertThrows<AccessDeniedException> {
      manager.changePassword("old", "new")
    }
  }

  @Test
  fun `createUser with null password throws NullPointerException`() {
    val user = User("no-pass-user", null, true, true, true, true,
      listOf(SimpleGrantedAuthority("ROLE_TERMINAL")))

    assertThrows<NullPointerException> {
      manager.createUser(user)
    }
  }

  @Test
  fun `updateUser with null password throws NullPointerException`() {
    val existing = testPrincipal(id = 1, name = "old-terminal", role = Role.TERMINAL)
    val user = User("old-terminal", null, false, true, true, true,
      listOf(SimpleGrantedAuthority("ROLE_TERMINAL")))
    every { principalRepository.findByName("old-terminal") } returns existing

    assertThrows<NullPointerException> {
      manager.updateUser(user)
    }
  }

  @Test
  fun `userExists returns true when found`() {
    every { principalRepository.findByName("exists") } returns testPrincipal(id = 1, name = "exists")
    assertTrue(manager.userExists("exists"))
  }

  @Test
  fun `userExists returns false when not found`() {
    every { principalRepository.findByName("nope") } returns null
    assertFalse(manager.userExists("nope"))
  }
}
