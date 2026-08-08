package hu.bme.sch.kirpay.event

import jakarta.persistence.*

@Entity
@Table(name = "events")
data class Event(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null,
  @Column(nullable = false)
  val event: String,
  @Column(nullable = false)
  val timestamp: Long,
  @Column(nullable = false)
  val message: String,
  @Column(nullable = false)
  val performedBy: String
)
