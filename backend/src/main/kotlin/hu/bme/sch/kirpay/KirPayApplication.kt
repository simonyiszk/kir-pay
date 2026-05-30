package hu.bme.sch.kirpay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableResilientMethods
@EnableMethodSecurity
@SpringBootApplication
@ConfigurationPropertiesScan
class KirPayApplication


fun main(args: Array<String>) {
  runApplication<KirPayApplication>(*args)
}
