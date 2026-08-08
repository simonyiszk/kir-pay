package hu.bme.sch.kirpay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import java.util.concurrent.Executor

@EnableResilientMethods
@EnableMethodSecurity(securedEnabled = true)
@EnableAsync
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
class KirPayApplication {
  @Bean
  fun taskExecutor(): Executor = SimpleAsyncTaskExecutor().apply {
    setVirtualThreads(true)
    setThreadNamePrefix("kirpay-async-")
    setTaskTerminationTimeout(30_000L)
  }
}

fun main(args: Array<String>) {
  runApplication<KirPayApplication>(*args)
}
