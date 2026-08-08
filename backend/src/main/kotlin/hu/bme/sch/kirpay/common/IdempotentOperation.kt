package hu.bme.sch.kirpay.common

import jakarta.persistence.*

@Entity
@Table(name = "idempotent_operations")
data class IdempotentOperation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false, unique = true)
  val idempotencyKey: String,
  @Column(nullable = false)
  val operationType: String,
  @Column(nullable = false)
  val requestFingerprint: String,
  @Column(nullable = false, columnDefinition = "text")
  val responseJson: String,
  @Column(nullable = false)
  val createdAt: Long
)
