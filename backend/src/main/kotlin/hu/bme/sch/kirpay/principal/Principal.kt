package hu.bme.sch.kirpay.principal

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Table
import org.springframework.modulith.ApplicationModule
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


enum class Role {
  ADMIN,
  TERMINAL
}


object PermissionName {
  const val UPLOAD_FUNDS = "UPLOAD_FUNDS"
  const val TRANSFER_FUNDS = "TRANSFER_FUNDS"
  const val SELL_ITEMS = "SELL_ITEMS"
  const val REDEEM_VOUCHERS = "REDEEM_VOUCHERS"
  const val ASSIGN_CARDS = "ASSIGN_CARDS"
}


enum class Permission {
  UPLOAD_FUNDS,
  TRANSFER_FUNDS,
  SELL_ITEMS,
  REDEEM_VOUCHERS,
  ASSIGN_CARDS
}


@ApplicationModule
@Table("principals")
data class Principal(
  @Id var id: Int?,
  val name: String,
  val secret: String,
  val role: Role,
  val active: Boolean,
  val canUpload: Boolean,
  val canTransfer: Boolean,
  val canSellItems: Boolean,
  val canRedeemVouchers: Boolean,
  val canAssignCards: Boolean,
  val createdAt: Long,
  val lastUsed: Long
) : UserDetails {

  @Transient
  @JsonIgnore
  override fun getAuthorities(): MutableCollection<out GrantedAuthority> = getPrincipalAuthorities(this)


  @Transient
  @JsonIgnore
  override fun getPassword(): String = secret


  @Transient
  @JsonIgnore
  override fun getUsername(): String = name


  @Transient
  @JsonIgnore
  override fun isEnabled(): Boolean = active


  @Transient
  @JsonIgnore
  override fun isAccountNonExpired(): Boolean = true


  @Transient
  @JsonIgnore
  override fun isAccountNonLocked(): Boolean = true


  // Sessions are stateless, so this shouldn't be an issue
  @Transient
  @JsonIgnore
  override fun isCredentialsNonExpired(): Boolean = true

}
