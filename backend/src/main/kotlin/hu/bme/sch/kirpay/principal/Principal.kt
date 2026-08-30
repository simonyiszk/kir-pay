package hu.bme.sch.kirpay.principal

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.io.Serializable

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

@Entity
@Table(name = "principals")
data class Principal(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false)
  val name: String,
  @Column(nullable = false)
  val secret: String,
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val role: Role,
  @Column(nullable = false)
  val active: Boolean,
  @Column(nullable = false)
  val canUpload: Boolean,
  @Column(nullable = false)
  val canTransfer: Boolean,
  @Column(nullable = false)
  val canSellItems: Boolean,
  @Column(nullable = false)
  val canRedeemVouchers: Boolean,
  @Column(nullable = false)
  val canAssignCards: Boolean,
  @Column(nullable = false)
  val createdAt: Long,
  @Column(nullable = false)
  val lastUsed: Long,
  @Version
  val version: Int = 0
) : UserDetails, Serializable {
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

  @Transient
  @JsonIgnore
  override fun isCredentialsNonExpired(): Boolean = true

}
