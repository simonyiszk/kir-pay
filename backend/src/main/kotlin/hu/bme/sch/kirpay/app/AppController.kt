package hu.bme.sch.kirpay.app

import hu.bme.sch.kirpay.common.APP_ENDPOINT
import hu.bme.sch.kirpay.common.InternalErrorException
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class AppController(private val appConfig: AppConfig) {

  data class AppResponse(val config: AppConfig, val principal: Principal)

  @GetMapping(APP_ENDPOINT)
  fun app() = AppResponse(
    // Had to copy because of Spring AOP proxy terribleness
    config = appConfig.copy(),
    // If this throws, it means that we introduced some serious regression into the application
    principal = getLoggedInPrincipal() ?: throw InternalErrorException("Hogy léptél be az alkalmazásba? :o")
  )

}
