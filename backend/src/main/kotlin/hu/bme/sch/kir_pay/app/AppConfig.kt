package hu.bme.sch.kir_pay.app

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.modulith.ApplicationModule


@ApplicationModule
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "hu.bme.sch.kir-pay.terminal")
data class AppConfig(
  val currencySymbol: String,
  val showUploadTab: Boolean = true,
  val showPayTab: Boolean = true,
  val showBalanceTab: Boolean = true,
  val showSetCardTab: Boolean = true,
  val showCartTab: Boolean = true,
  val showTokenTab: Boolean = true,
  val showTransferTab: Boolean = true
)
