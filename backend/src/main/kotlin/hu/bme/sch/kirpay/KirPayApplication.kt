package hu.bme.sch.kirpay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import java.util.concurrent.Executor

@EnableResilientMethods
@EnableMethodSecurity(securedEnabled = true)
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
class KirPayApplication {

  @Bean
  fun taskExecutor(): Executor = ThreadPoolTaskExecutor().apply {
    corePoolSize = 4
    maxPoolSize = 8
    queueCapacity = 100
    setThreadNamePrefix("kirpay-async-")
    setWaitForTasksToCompleteOnShutdown(true)
    initialize()
  }
}


fun main(args: Array<String>) {
  runApplication<KirPayApplication>(*args)
}
