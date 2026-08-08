package hu.bme.sch.kirpay.event

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventRepository : JpaRepository<Event, Int> {
  @Query(value = "select * from events order by timestamp desc", nativeQuery = true)
  fun findAllOrderByTimestampDesc(): List<Event>

  @Query(value = "select * from events order by timestamp desc, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Event>

}
