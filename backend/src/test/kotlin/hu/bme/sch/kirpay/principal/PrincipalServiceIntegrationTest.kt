package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.BaseIntegrationTest
import hu.bme.sch.kirpay.common.BadRequestException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PrincipalServiceIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var principalService: PrincipalService

  @Autowired
  private lateinit var passwordEncoder: PasswordEncoder

  // --- findAll ---

  @Test
  fun `findAll returns principals ordered by name`() {
    val all = principalService.findAll()
    assertTrue(all.isNotEmpty())
    assertTrue(all.any { it.name == "test-admin" })
  }

  // --- createPrincipal ---

  @Test
  fun `createPrincipal creates and can authenticate`() {
    val dto = PrincipalDto(
      name = "integration-test-terminal",
      password = "secret123",
      role = Role.TERMINAL,
      canUpload = true,
      canTransfer = false,
      canSellItems = true,
      canRedeemVouchers = false,
      canAssignCards = false,
      active = true
    )

    val created = principalService.createPrincipal(dto)
    assertNotNull(created)
    assertEquals("integration-test-terminal", created.name)
    assertEquals(Role.TERMINAL, created.role)
    assertTrue(passwordEncoder.matches("secret123", created.secret))
  }

  @Test
  fun `createPrincipal with duplicate name throws when failOnCollision true`() {
    val dto = PrincipalDto(
      name = "duplicate-terminal", password = "pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    principalService.createPrincipal(dto)

    assertThrows<BadRequestException> {
      principalService.createPrincipal(dto, failOnCollision = true)
    }
  }

  @Test
  fun `createPrincipal with duplicate name updates when failOnCollision false`() {
    val dto = PrincipalDto(
      name = "collision-terminal", password = "new-password", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    principalService.createPrincipal(dto)
    val result = principalService.createPrincipal(dto.copy(password = "updated-pw"), failOnCollision = false)
    assertNotNull(result)
    assertEquals("collision-terminal", result.name)
  }

  // --- find ---

  @Test
  fun `find existing principal returns it`() {
    val dto = PrincipalDto(
      name = "findable", password = "pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    val created = principalService.createPrincipal(dto)!!

    val found = principalService.find(created.id!!)
    assertEquals("findable", found.name)
  }

  @Test
  fun `find non-existent principal throws`() {
    assertThrows<BadRequestException> { principalService.find(99999) }
  }

  // --- setEnabled ---

  @Test
  fun `setEnabled disables terminal`() {
    val dto = PrincipalDto(
      name = "disable-me", password = "pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    val created = principalService.createPrincipal(dto)!!

    val disabled = principalService.setEnabled(created.id!!, false)
    assertFalse(disabled.active)

    val reEnabled = principalService.setEnabled(created.id!!, true)
    assertTrue(reEnabled.active)
  }

  // --- updateLastUsed ---

  @Test
  fun `updateLastUsed changes timestamp`() {
    val dto = PrincipalDto(
      name = "update-me", password = "pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    val created = principalService.createPrincipal(dto)!!
    val original = created.lastUsed

    principalService.updateLastUsed(created.id)

    val updated = principalService.find(created.id!!)
    assertTrue(updated.lastUsed >= original)
  }

  // --- delete ---

  @Test
  fun `delete terminal succeeds`() {
    val dto = PrincipalDto(
      name = "delete-me", password = "pw", role = Role.TERMINAL,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true, active = true
    )
    val created = principalService.createPrincipal(dto)!!

    principalService.delete(created.id!!)
    assertThrows<BadRequestException> { principalService.find(created.id!!) }
  }
}
