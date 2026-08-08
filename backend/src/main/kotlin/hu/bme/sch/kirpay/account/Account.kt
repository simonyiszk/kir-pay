package hu.bme.sch.kirpay.account

import jakarta.persistence.*
import java.math.BigInteger

@Entity
@Table(name = "accounts")
data class Account(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false)
  val name: String,
  val email: String?,
  val phone: String?,
  @Column(unique = true)
  val card: String?,
  @Column(nullable = false, precision = 38)
  val balance: BigInteger,
  @Column(nullable = false)
  val active: Boolean,
  @Version
  val version: Int = 0
)
