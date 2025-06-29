package hu.bme.sch.kirpay.account

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface AccountRepository : CrudRepository<Account, Int> {

  fun findByCard(card: String): Account?


  @Query("select * from accounts order by name")
  fun findAllOrderByName(): List<Account>


  @Query("select * from accounts where card = :card and active")
  fun findActiveAccountByCard(card: String): Account?


  @Query("select * from accounts where id = :id and active")
  fun findActiveAccountById(id: Int): Account?


  @Query("select coalesce(sum(balance), 0) from accounts where active")
  fun getAllActiveBalance(): Long

}
