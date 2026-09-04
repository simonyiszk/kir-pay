package hu.bme.sch.kirpay.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface OrderRepository : JpaRepository<Order, Int> {
  @Query(value = "select * from orders where idempotency_key = :key", nativeQuery = true)
  fun findByIdempotencyKey(key: String): Order?

  @Query(value = "select * from orders order by timestamp desc", nativeQuery = true)
  fun findAllOrderByTimestampDesc(): List<Order>

  @Query(value = "select * from orders order by timestamp desc, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Order>

  @Query(value =
    """select o.id  as order_id,
              o.account_id,
              o.timestamp,
              ol.id as order_line_id,
              ol.item_id,
              ol.item_count,
              ol.message,
              ol.used_voucher,
              ol.paid_amount
       from orders o
                inner join order_lines ol on o.id = ol.order_id
       order by o.timestamp desc, ol.id desc""", nativeQuery = true
  )
  fun findAllOrderWithOrderLinesOrderByTimestampDesc(): List<OrderWithOrderLine>

  @Query(value =
    """select o.id  as order_id,
               o.account_id,
               o.timestamp,
               ol.id as order_line_id,
               ol.item_id,
               ol.item_count,
               ol.message,
               ol.used_voucher,
               ol.paid_amount
        from orders o
                 inner join order_lines ol on o.id = ol.order_id
        order by o.timestamp desc, o.id desc, ol.id asc
        offset :skip rows fetch next :take rows only""", nativeQuery = true
  )
  fun findAllOrderWithOrderLinesOrderByTimestampDescPaginated(skip: Long, take: Int): List<OrderWithOrderLine>

  @Query(value =
    """select a.id as account_id,
               a.name,
               a.email,
               coalesce(sum(ol.item_count), 0) as item_count
        from accounts a
                 inner join orders o on o.account_id = a.id
                 inner join order_lines ol on ol.order_id = o.id
                 inner join items i on i.id = ol.item_id
        where i.enabled = true
          and i.show_on_leaderboard = true
          and a.active = true
        group by a.id, a.name, a.email
        order by item_count desc
        limit :limit""", nativeQuery = true
  )
  fun findConsumptionLeaderboard(limit: Int): List<ConsumptionLeaderboardEntry>

}

@Repository
interface ItemRepository : JpaRepository<Item, Int> {
  fun findByEnabledOrderByName(enabled: Boolean): List<Item>

  @Query(value = "select * from items order by name asc", nativeQuery = true)
  fun findAllOrderByName(): List<Item>

  @Query(value = "select * from items order by name asc, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByNamePaginated(skip: Long, take: Int): List<Item>

}

@Repository
interface VoucherRepository : JpaRepository<Voucher, Int> {
  @Query(value = "select * from vouchers where account_id = :accountId and item_id = :itemId", nativeQuery = true)
  fun findByAccountAndItem(accountId: Int, itemId: Int): Voucher?

  @Query(value = """
    select vouchers.id as voucher_id, account_id, item_id, i.name as item_name, vouchers.count
    from vouchers
         inner join items i on i.id = vouchers.item_id
    where account_id = :accountId
  """, nativeQuery = true)
  fun findAllByAccountIdWithItemName(accountId: Int): List<VoucherWithItemName>

  @Query(value = "select * from vouchers order by account_id", nativeQuery = true)
  fun findAllOrderByAccountId(): List<Voucher>

  @Query(value = "select * from vouchers order by account_id, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByAccountIdPaginated(skip: Long, take: Int): List<Voucher>

  @Modifying(clearAutomatically = true)
  @Query(value = "update vouchers set count = count + :delta, version = version + 1 where id = :id and version = :version",
    nativeQuery = true)
  fun incrementCount(id: Int, delta: Int, version: Int): Int

}

@Repository
interface OrderLineRepository : JpaRepository<OrderLine, Int> {
  @Query(value = "select * from order_lines order by order_id desc", nativeQuery = true)
  fun findAllOrderByOrderIdDesc(): List<OrderLine>

  @Query(value = "select * from order_lines order by order_id desc, id desc offset :skip rows fetch next :take rows only",
    nativeQuery = true)
  fun findAllOrderByOrderIdDescPaginated(skip: Long, take: Int): List<OrderLine>

  @Query(value = """
    select i.id as item_id, i.name as item_name, sum(o.item_count) as item_count
    from items i
             inner join order_lines o on i.id = o.item_id
    where i.enabled = true
      and i.show_on_leaderboard = true
    group by i.id
    order by item_count desc
    limit :limit
  """, nativeQuery = true)
  fun getItemConsumptionLeaderboard(limit: Int): List<ItemConsumptionLeaderboardEntry>
}
