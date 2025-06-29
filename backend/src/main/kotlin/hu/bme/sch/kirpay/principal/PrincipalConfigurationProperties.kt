package hu.bme.sch.kirpay.principal

import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.Transactional


@ConfigurationProperties(prefix = "hu.bme.sch.kirpay")
data class ImportedPrincipals(val principals: List<PrincipalDto>)


@Configuration
class PrincipalImporter(
  private val principals: ImportedPrincipals,
  private val principalService: PrincipalService
) {

  @Bean
  @Transactional
  fun importPrincipals(): ApplicationRunner = ApplicationRunner {
    principals.principals.forEach { principalService.createPrincipal(it, failOnCollision = false) }
  }

}


data class PrincipalDto(
  val name: String,
  val password: String,
  val role: Role,
  val canUpload: Boolean,
  val canTransfer: Boolean,
  val canSellItems: Boolean,
  val canRedeemVouchers: Boolean,
  val canAssignCards: Boolean,
  val active: Boolean,
)
