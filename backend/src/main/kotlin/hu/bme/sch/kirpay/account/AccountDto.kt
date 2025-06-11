package hu.bme.sch.kirpay.account


data class AccountCreateDto(
  val id: Int?,
  val name: String,
  val email: String?,
  val phone: String?,
  val card: String?,
  val balance: Long,
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
  val name: String,
  val email: String?,
  val phone: String?,
  val card: String?,
  val balance: Long,
  val active: Boolean
) {
  fun toAccount(id: Int) = Account(
    id = id,
    name = name,
    email = email,
    phone = phone,
    card = card,
    balance = balance,
    active = active
  )
}
