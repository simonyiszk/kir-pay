package hu.bme.sch.kirpay.common

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LoginController(
  private val authenticationConfiguration: AuthenticationConfiguration,
) {
  private val securityContextRepository = HttpSessionSecurityContextRepository()
  private val logoutHandler = SecurityContextLogoutHandler()

  data class LoginRequest(val username: String, val password: String)

  @PostMapping("/v1/api/login")
  fun login(
    @RequestBody request: LoginRequest,
    httpRequest: HttpServletRequest,
    httpResponse: HttpServletResponse,
  ): ResponseEntity<Void> {
    val auth = authenticationConfiguration.getAuthenticationManager().authenticate(
      UsernamePasswordAuthenticationToken(request.username, request.password)
    )
    val context = SecurityContextHolder.createEmptyContext()
    context.authentication = auth
    SecurityContextHolder.setContext(context)
    securityContextRepository.saveContext(context, httpRequest, httpResponse)
    return ResponseEntity.noContent().build()
  }

  @PostMapping("/v1/api/logout")
  fun logout(request: HttpServletRequest, response: HttpServletResponse) {
    logoutHandler.logout(request, response, SecurityContextHolder.getContext().authentication)
  }
}
