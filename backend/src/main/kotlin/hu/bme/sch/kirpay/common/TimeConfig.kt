package hu.bme.sch.kirpay.common

import hu.bme.sch.kirpay.app.AppConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId


@Configuration
class TimeConfig(private val appConfig: AppConfig) {
  @Bean
  fun clock(): Clock = Clock.system(ZoneId.of(appConfig.timeZone))

}
