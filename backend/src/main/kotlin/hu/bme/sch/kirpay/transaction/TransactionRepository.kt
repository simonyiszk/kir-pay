package hu.bme.sch.kirpay.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigInteger

@Repository
interface TransactionRepository : JpaRepository<Transaction, Int> {
  @Query(value = "select * from transactions order by timestamp desc", nativeQuery = true)
  fun findAllOrderByTimestampDesc(): List<Transaction>

  @Query(value = "select * from transactions order by timestamp desc, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Transaction>

  @Query(value = "select coalesce(sum(amount), 0) from transactions where type = 'CHARGE'", nativeQuery = true)
  fun getIncome(): BigInteger

  @Query(value = "select coalesce(sum(amount), 0) from transactions", nativeQuery = true)
  fun getTransactionVolume(): BigInteger

  @Query(value = "select coalesce(sum(amount), 0) from transactions where type = 'TOP_UP'", nativeQuery = true)
  fun getAllUploads(): BigInteger

  @Modifying(clearAutomatically = true)
  @Query(value = """
    insert into transactions (type, sender_id, recipient_id, amount, message, timestamp, fingerprint)
    values (:type, :senderId, :recipientId, :amount, :message, :timestamp, :fingerprint)
    on conflict (fingerprint) do nothing
  """, nativeQuery = true)
  fun saveIgnoreDuplicate(
    type: String,
    senderId: Int?,
    recipientId: Int?,
    amount: BigInteger,
    message: String?,
    timestamp: Long,
    fingerprint: String
  ): Int
}
