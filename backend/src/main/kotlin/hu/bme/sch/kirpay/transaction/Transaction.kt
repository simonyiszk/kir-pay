package hu.bme.sch.kirpay.transaction

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.modulith.ApplicationModule


@ApplicationModule
@Table("transactions")
data class Transaction(
  @Id @Column() var id: Int?,
  val type: TransactionType,
  val senderId: Int?,
  val recipientId: Int?,
  val amount: Long,
  val message: String?,
  val timestamp: Long
)


enum class TransactionType {
  TOP_UP,
  TRANSFER,
  CHARGE
}
