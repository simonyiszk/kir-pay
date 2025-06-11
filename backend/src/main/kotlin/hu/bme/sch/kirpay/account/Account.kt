package hu.bme.sch.kirpay.account

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.modulith.ApplicationModule


@ApplicationModule
@Table("accounts")
data class Account(
  @Id var id: Int?,
  val name: String,
  val email: String?,
  val phone: String?,
  val card: String?,
  val balance: Long,
  val active: Boolean
)
