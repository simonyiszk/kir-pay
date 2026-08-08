package hu.bme.sch.kirpay.principal

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PrincipalRepository : JpaRepository<Principal, Int> {
  fun findByName(name: String): Principal?

  @Query(value = "select * from principals order by name", nativeQuery = true)
  fun findAllOrderByName(): List<Principal>

  @Modifying(clearAutomatically = true)
  @Query(value = "update principals set last_used = :ts where id = :id", nativeQuery = true)
  fun updateLastUsed(id: Int, ts: Long)

}
