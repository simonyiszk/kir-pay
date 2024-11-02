package hu.bme.sch.kir_pay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableMethodSecurity
@ConfigurationPropertiesScan
class KirPayApplication


fun main(args: Array<String>) {
  runApplication<KirPayApplication>(*args)
}