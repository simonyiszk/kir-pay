package hu.bme.sch.kirpay.transaction

import jakarta.persistence.*
import java.math.BigInteger

@Entity
@Table(name = "transactions")
data class Transaction(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  val type: TransactionType,
  val senderId: Int?,
  val recipientId: Int?,
  @Column(nullable = false, precision = 38)
  val amount: BigInteger,
  val message: String?,
  @Column(nullable = false)
  val timestamp: Long,
  @Column(nullable = false, unique = true)
  val fingerprint: String
)

enum class TransactionType {
  TOP_UP,
  TRANSFER,
  CHARGE
}
