package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.testPrincipal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtilTest {

  @Test
  fun `getPrincipalAuthorities returns ROLE_TERMINAL plus permission authorities for terminal`() {
    val principal = testPrincipal(role = Role.TERMINAL)
    val authorities = getPrincipalAuthorities(principal)

    assertTrue(authorities.any { it.authority == "ROLE_TERMINAL" },
      "Should contain ROLE_TERMINAL")
    assertTrue(authorities.any { it.authority == "SELL_ITEMS" },
      "Should contain SELL_ITEMS")
    assertTrue(authorities.any { it.authority == "UPLOAD_FUNDS" },
      "Should contain UPLOAD_FUNDS")
  }

  // BUG-07-T2
  @Test
  fun `copyWithAuthorities with ROLE_ADMIN and all permissions sets all flags true`() {
    val principal = testPrincipal(role = Role.TERMINAL, canSellItems = false, canUpload = false)
    val authorities = listOf(
      SimpleGrantedAuthority("ROLE_ADMIN"),
      SimpleGrantedAuthority("ROLE_SELL_ITEMS"),
      SimpleGrantedAuthority("ROLE_UPLOAD_FUNDS"),
      SimpleGrantedAuthority("ROLE_REDEEM_VOUCHERS"),
      SimpleGrantedAuthority("ROLE_TRANSFER_FUNDS"),
      SimpleGrantedAuthority("ROLE_ASSIGN_CARDS")
    )

    val result = principal.copyWithAuthorities(authorities)

    assertEquals(Role.ADMIN, result.role, "Role should be ADMIN")
    assertTrue(result.canSellItems, "canSellItems should be true")
    assertTrue(result.canUpload, "canUpload should be true")
    assertTrue(result.canRedeemVouchers, "canRedeemVouchers should be true")
    assertTrue(result.canTransfer, "canTransfer should be true")
    assertTrue(result.canAssignCards, "canAssignCards should be true")
  }

  // BUG-07-T3
  @Test
  fun `copyWithAuthorities with ROLE_TERMINAL and SELL_ITEMS only sets only that flag`() {
    val principal = testPrincipal(
      role = Role.TERMINAL,
      canSellItems = false,
      canUpload = false,
      canTransfer = false,
      canRedeemVouchers = false,
      canAssignCards = false
    )
    val authorities = listOf(
      SimpleGrantedAuthority("ROLE_TERMINAL"),
      SimpleGrantedAuthority("ROLE_SELL_ITEMS")
    )

    val result = principal.copyWithAuthorities(authorities)

    assertEquals(Role.TERMINAL, result.role, "Role should be TERMINAL")
    assertTrue(result.canSellItems, "canSellItems should be true")
    assertFalse(result.canUpload, "canUpload should be false")
    assertFalse(result.canTransfer, "canTransfer should be false")
    assertFalse(result.canRedeemVouchers, "canRedeemVouchers should be false")
    assertFalse(result.canAssignCards, "canAssignCards should be false")
  }

  // BUG-07-T4
  @Test
  fun `copyWithAuthorities with no recognized role throws IllegalArgumentException`() {
    val principal = testPrincipal()
    val authorities = listOf(SimpleGrantedAuthority("ROLE_UNKNOWN"))

    assertThrows<IllegalArgumentException> {
      principal.copyWithAuthorities(authorities)
    }
  }
}
