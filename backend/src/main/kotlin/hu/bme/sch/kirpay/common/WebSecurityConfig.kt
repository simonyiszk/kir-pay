package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.app.BackendConfig
import hu.bme.sch.kirpay.principal.Role
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.header.Header
import org.springframework.security.web.header.writers.StaticHeadersWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain =
    http.authorizeHttpRequests {
      it.requestMatchers("/actuator/health/liveness", "/actuator/health/readiness").permitAll()
      it.requestMatchers("/actuator/**").hasRole(Role.ADMIN.name)
      it.requestMatchers("/error").permitAll()
      it.requestMatchers("/v1/api/login", "/v1/api/logout").permitAll()
      it.requestMatchers(APP_ENDPOINT).hasRole(Role.TERMINAL.name)
      it.requestMatchers("$TERMINAL_API/**").hasRole(Role.TERMINAL.name)
      it.anyRequest().hasRole(Role.ADMIN.name)
    }
      .cors(Customizer.withDefaults())
      .csrf { it.disable() }
      .securityContext { it.requireExplicitSave(true) }
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
      .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
      .headers { headers ->
        headers.addHeaderWriter(
          StaticHeadersWriter(
            listOf(
              Header("Referrer-Policy", "strict-origin-when-cross-origin"),
            )
          )
        )
      }
      .build()

  @Bean
  fun roleHierarchy(): RoleHierarchy = RoleHierarchyImpl.withDefaultRolePrefix()
    .role(Role.ADMIN.name).implies(Role.TERMINAL.name)
    .build()

  @Bean
  fun corsConfigurationSource(backendConfig: BackendConfig): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.allowedOrigins = listOf(backendConfig.frontendUrl)
    configuration.allowedMethods = listOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    configuration.allowedHeaders = listOf("authorization", "content-type")
    configuration.allowCredentials = true
    return UrlBasedCorsConfigurationSource().also { it.registerCorsConfiguration("/**", configuration) }
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

}
