package hu.bme.sch.kirpay.account

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank


data class AccountCreateDto(
  val id: Int?,
  @field:NotBlank val name: String,
  val email: String?,
  val phone: String?,
  val card: String?,
  @field:Min(0) val balance: Long,
  val active: Boolean
) {
  fun toAccount() = Account(
    id = id,
    name = name,
    email = email,
    phone = phone,
    card = card,
    balance = balance,
    active = active
  )
}


data class AccountUpdateDto(
  @field:NotBlank val name: String,
  val email: String?,
  val phone: String?,
  val card: String?,
  val active: Boolean
) {
  fun toAccount(id: Int, existingBalance: Long, existingVersion: Int) = Account(
    id = id,
    name = name,
    email = email,
    phone = phone,
    card = card,
    balance = existingBalance,
    active = active,
    version = existingVersion
  )
}
