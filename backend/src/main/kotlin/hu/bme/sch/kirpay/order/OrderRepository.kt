package hu.bme.sch.kirpay.order

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface OrderRepository : CrudRepository<Order, Int> {

  @Query("select * from orders order by timestamp desc")
  fun findAllOrderByTimestampDesc(): List<Order>


  @Query("select * from orders order by timestamp desc offset :skip rows fetch next :take rows only")
  fun findAllOrderByTimestampDescPaginated(skip: Long, take: Int): List<Order>


  @Query(
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
       order by o.timestamp desc, ol.id desc"""
  )
  fun findAllOrderWithOrderLinesOrderByTimestampDesc(): List<OrderWithOrderLine>


  @Query(
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
       order by o.timestamp desc, ol.id asc
       offset :skip rows fetch next :take rows only"""
  )
  fun findAllOrderWithOrderLinesOrderByTimestampDescPaginated(skip: Long, take: Int): List<OrderWithOrderLine>

}


@Repository
interface ItemRepository : CrudRepository<Item, Int> {

  fun findByEnabledOrderByName(enabled: Boolean): List<Item>


  @Query("select * from items order by name asc")
  fun findAllOrderByName(): List<Item>


  @Query("select * from items order by name asc offset :skip rows fetch next :take rows only")
  fun findAllOrderByNamePaginated(skip: Long, take: Int): List<Item>

}


@Repository
interface VoucherRepository : CrudRepository<Voucher, Int> {

  @Query("select * from vouchers where account_id = :accountId and item_id = :itemId")
  fun findByAccountAndItem(accountId: Int, itemId: Int): Voucher?


  @Query("select * from vouchers order by account_id")
  fun findAllOrderByAccountId(): List<Voucher>


  @Query("select * from vouchers order by account_id offset :skip rows fetch next :take rows only")
  fun findAllOrderByAccountIdPaginated(skip: Long, take: Int): List<Voucher>

}


@Repository
interface OrderLineRepository : CrudRepository<OrderLine, Int> {

  @Query("select * from order_lines order by order_id desc")
  fun findAllOrderByOrderIdDesc(): List<OrderLine>


  @Query("select * from order_lines order by order_id desc offset :skip rows fetch next :take rows only")
  fun findAllOrderByOrderIdDescPaginated(skip: Long, take: Int): List<OrderLine>

}
