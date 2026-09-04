package hu.bme.sch.kirpay.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
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

  @Query(value = """
    with buckets as (
      select ts::date                                 as day,
             extract(hour from ts)::int               as hour,
             sum(t.amount)                            as revenue
      from transactions t
               cross join lateral (select to_timestamp(t.timestamp / 1000.0) at time zone :zone as ts) x
      where t.type = 'CHARGE'
        and t.timestamp >= :windowStartMs
      group by day, hour
    ),
    grid as (
      select (to_timestamp(:windowStartMs / 1000.0) at time zone :zone + make_interval(days => d))::date as day,
             h as hour
      from generate_series(0, :days - 1) as d
               cross join generate_series(0, 23) as h
    )
    select to_char(x.day, 'YYYY-MM-DD') as date,
           x.hour                       as hour,
           sum(x.revenue)               as revenue
    from (select day, hour, revenue from buckets
          union all
          select day, hour, 0::numeric as revenue from grid) x
    group by x.day, x.hour
    order by x.day desc, x.hour
  """, nativeQuery = true)
  fun findRevenueHeatmap(windowStartMs: Long, days: Int, zone: String): List<RevenueHeatmapEntry>

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
