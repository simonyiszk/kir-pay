package hu.bme.sch.kirpay.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jdbc.core.dialect.JdbcDialect
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect

@Configuration
@Profile("cds-training")
class CdsTrainingConfig {

  @Bean
  fun jdbcDialect(): JdbcDialect {
    return JdbcPostgresDialect.INSTANCE
  }
}
