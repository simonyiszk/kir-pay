package hu.bme.sch.kirpay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@EnableRetry
@EnableMethodSecurity
@SpringBootApplication
@ConfigurationPropertiesScan
class KirPayApplication


fun main(args: Array<String>) {
  runApplication<KirPayApplication>(*args)
}
