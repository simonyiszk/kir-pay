package hu.bme.sch.kirpay.app

import com.fasterxml.jackson.databind.ObjectMapper
import hu.bme.sch.kirpay.principal.Role
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppControllerTest {

  private val objectMapper = ObjectMapper()

  @Test
  fun `AppResponse with PrincipalResponse has no secret in JSON`() {
    val principalResponse = PrincipalResponse(
      id = 1, name = "test-user", role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = false, canSellItems = true,
      canRedeemVouchers = false, canAssignCards = true,
      createdAt = 1700000000000L, lastUsed = 1700000001000L
    )
    val appConfig = AppConfig(
      currencySymbol = "JMF",
      showUploadTab = true
    )
    val appResponse = AppController.AppResponse(config = appConfig, principal = principalResponse)

    val json = objectMapper.writeValueAsString(appResponse)

    // Verify JSON does NOT contain secret
    assertFalse(json.contains("\"secret\""),
      "Full app response JSON must not contain 'secret': $json")
    assertTrue(json.contains("\"currencySymbol\""), "Should contain config: $json")
    assertTrue(json.contains("\"name\""), "Should contain principal name: $json")
  }
}
