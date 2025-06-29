package hu.bme.sch.kirpay.app

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@EnableConfigurationProperties
@ConfigurationProperties(prefix = "hu.bme.sch.kirpay.backend")
data class BackendConfig(val frontendUrl: String)
