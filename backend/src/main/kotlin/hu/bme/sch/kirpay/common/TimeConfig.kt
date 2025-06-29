package hu.bme.sch.kirpay.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock


@Configuration
class TimeConfig {

  @Bean
  fun clock(): Clock = Clock.systemDefaultZone()

}
