package hu.bme.sch.kirpay.event

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface EventRepository : CrudRepository<Event, Int> {

  @Query("select * from events order by timestamp desc")
  fun findAllOrderByTimestampDesc(): List<Event>


  @Query("select * from events order by timestamp desc offset :skip rows fetch next :take rows only")
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Event>

}
