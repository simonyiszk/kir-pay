package hu.bme.sch.kirpay.principal

import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrincipalJsonSerializationTest {

  private val mapper: ObjectMapper = jacksonObjectMapper()

  private val principal = Principal(
    id = 1,
    name = "test-user",
    secret = "super-secret-password",
    role = Role.TERMINAL,
    active = true,
    canUpload = true,
    canTransfer = false,
    canSellItems = true,
    canRedeemVouchers = false,
    canAssignCards = true,
    createdAt = 1700000000000L,
    lastUsed = 1700000001000L
  )

  @Test
  fun `UserDetails getter methods are excluded from JSON`() {
    val json = mapper.writeValueAsString(principal)

    assertFalse(json.contains("\"password\""), "getPassword() must not be serialized as 'password'")
    assertFalse(json.contains("\"username\""), "getUsername() must not be serialized as 'username'")
    assertFalse(json.contains("\"authorities\""), "getAuthorities() must not be serialized as 'authorities'")
    assertFalse(json.contains("\"enabled\""), "isEnabled() must not be serialized as 'enabled'")
    assertFalse(json.contains("\"accountNonExpired\""), "isAccountNonExpired() must not be serialized")
    assertFalse(json.contains("\"accountNonLocked\""), "isAccountNonLocked() must not be serialized")
    assertFalse(json.contains("\"credentialsNonExpired\""), "isCredentialsNonExpired() must not be serialized")
  }

  @Test
  fun `basic fields are included in JSON`() {
    val json = mapper.writeValueAsString(principal)
    assertTrue(json.contains("\"name\":\"test-user\""), "Name must appear in JSON")
    assertTrue(json.contains("\"role\":\"TERMINAL\""), "Role must appear in JSON")
    assertTrue(json.contains("\"active\":true"), "Active must appear")
  }

  @Test
  fun `deserialized principal has correct values`() {
    val json = mapper.writeValueAsString(principal)
    val deserialized = mapper.readValue(json, Principal::class.java)
    assertEquals(principal.id, deserialized.id)
    assertEquals(principal.name, deserialized.name)
    assertEquals(principal.role, deserialized.role)
    assertEquals(principal.secret, deserialized.secret)
    assertEquals(principal.active, deserialized.active)
  }
}
