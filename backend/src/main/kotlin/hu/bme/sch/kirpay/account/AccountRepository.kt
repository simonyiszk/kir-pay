package hu.bme.sch.kirpay.account

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigInteger

@Repository
interface AccountRepository : JpaRepository<Account, Int> {
  fun findByCard(card: String): Account?

  @Query(value = "select * from accounts order by name", nativeQuery = true)
  fun findAllOrderByName(): List<Account>

  @Query(value = "select * from accounts where active order by name", nativeQuery = true)
  fun findAllActiveOrderByName(): List<Account>

  @Query(value = "select * from accounts where card = :card and active", nativeQuery = true)
  fun findActiveAccountByCard(card: String): Account?

  @Query(value = "select * from accounts where id = :id and active", nativeQuery = true)
  fun findActiveAccountById(id: Int): Account?

  @Query(value = "select * from accounts where email = :email and active", nativeQuery = true)
  fun findActiveAccountByEmail(email: String): Account?

  @Query(value = "select coalesce(sum(balance), 0) from accounts where active", nativeQuery = true)
  fun getAllActiveBalance(): BigInteger

  @Modifying(clearAutomatically = true)
  @Query(value = "delete from accounts where id = :id and balance = 0 and version = :version", nativeQuery = true)
  fun safeDelete(id: Int, version: Int): Int

}
