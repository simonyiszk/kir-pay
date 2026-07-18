package hu.bme.sch.kirpay.order

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class OrderLineService(private val orderLineRepository: OrderLineRepository) {

  fun getConsumptionLeaderboardByItem(limit: Int) = orderLineRepository.getItemConsumptionLeaderboard(if (limit < 0) Int.MAX_VALUE else limit)


  fun save(orderLine: OrderLine): OrderLine = orderLineRepository.save(orderLine)


  fun findAll() = orderLineRepository.findAllOrderByOrderIdDesc()


  fun findPaginated(page: Int, size: Int) =
    orderLineRepository.findAllOrderByOrderIdDescPaginated(page.toLong() * size, size)

}
