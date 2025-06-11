package hu.bme.sch.kirpay.event

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.modulith.ApplicationModule


@ApplicationModule
@Table("events")
data class Event(
  @Id var id: Int?,
  val event: String,
  val timestamp: Long,
  val message: String,
  val performedBy: String
)
