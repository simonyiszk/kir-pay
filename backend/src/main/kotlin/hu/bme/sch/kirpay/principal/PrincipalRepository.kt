package hu.bme.sch.kirpay.principal

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface PrincipalRepository : CrudRepository<Principal, Int> {

  fun findByName(name: String): Principal?


  @Query("select * from principals order by name")
  fun findAllOrderByName(): List<Principal>


  @Modifying
  @Query("update principals set last_used = :ts where id = :id")
  fun updateLastUsed(id: Int, ts: Long)

}
