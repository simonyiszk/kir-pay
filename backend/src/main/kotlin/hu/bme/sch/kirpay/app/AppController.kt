package hu.bme.sch.kirpay.app

import hu.bme.sch.kirpay.common.APP_ENDPOINT
import hu.bme.sch.kirpay.common.InternalErrorException
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class AppController(private val appConfig: AppConfig) {
  data class AppResponse(val config: AppConfig, val principal: PrincipalResponse)

  @GetMapping(APP_ENDPOINT)
  fun app(): AppResponse {
    val principal = getLoggedInPrincipal()
      ?: throw InternalErrorException("No authenticated principal found for the current session")
    return AppResponse(
      config = appConfig,
      principal = PrincipalResponse.fromPrincipal(principal)
    )
  }

}
