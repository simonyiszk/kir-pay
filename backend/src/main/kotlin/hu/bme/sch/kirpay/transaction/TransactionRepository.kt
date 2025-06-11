package hu.bme.sch.kirpay.transaction

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface TransactionRepository : CrudRepository<Transaction, Int> {

  @Query("select * from transactions order by timestamp desc")
  fun findAllOrderByTimestampDesc(): List<Transaction>


  @Query("select * from transactions order by timestamp desc offset :skip rows fetch next :take rows only")
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Transaction>


  @Query("select coalesce(sum(amount), 0) from transactions where type = 'CHARGE'")
  fun getIncome(): Long


  @Query("select coalesce(sum(amount), 0) from transactions")
  fun getTransactionVolume(): Long


  @Query("select coalesce(sum(amount), 0) from transactions where type = 'TOP_UP'")
  fun getAllUploads(): Long

}
