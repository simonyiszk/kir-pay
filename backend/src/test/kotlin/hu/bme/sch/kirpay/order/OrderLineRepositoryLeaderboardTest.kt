package hu.bme.sch.kirpay.order

import hu.bme.sch.kirpay.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import kotlin.test.assertEquals

class OrderLineRepositoryLeaderboardTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var orderRepository: OrderRepository

  @Autowired
  private lateinit var orderLineRepository: OrderLineRepository

  @Test
  fun `item leaderboard binds columns to dto correctly`() {
    val item = itemRepository.save(createItem(name = "Board").copy(showOnLeaderboard = true))
    val account = createAccount()
    val order = orderRepository.save(Order(accountId = account.id!!, timestamp = 1L))
    orderLineRepository.save(
      OrderLine(
        orderId = order.id,
        itemId = item.id,
        itemCount = 4,
        message = null,
        usedVoucher = false,
        paidAmount = BigInteger.valueOf(400)
      )
    )
    orderLineRepository.flush()

    val leaderboard = orderLineRepository.getItemConsumptionLeaderboard(10)
    assertEquals(1, leaderboard.size)
    assertEquals(item.id, leaderboard[0].itemId)
    assertEquals("Board", leaderboard[0].itemName)
    assertEquals(4L, leaderboard[0].itemCount)
  }

  @Test
  fun `item counts are summed across order lines`() {
    val item = itemRepository.save(createItem(name = "Board").copy(showOnLeaderboard = true))
    val account = createAccount()
    val order = orderRepository.save(Order(accountId = account.id!!, timestamp = 1L))
    listOf(2, 3).forEach { count ->
      orderLineRepository.save(
        OrderLine(
          orderId = order.id,
          itemId = item.id,
          itemCount = count,
          message = null,
          usedVoucher = false,
          paidAmount = BigInteger.valueOf(100L * count)
        )
      )
    }
    orderLineRepository.flush()

    val leaderboard = orderLineRepository.getItemConsumptionLeaderboard(10)
    assertEquals(1, leaderboard.size)
    assertEquals(5L, leaderboard[0].itemCount)
  }

  @Test
  fun `non-leaderboard and disabled items are excluded`() {
    val shown = itemRepository.save(createItem(name = "Shown").copy(showOnLeaderboard = true))
    val hidden = createItem(name = "Hidden") // showOnLeaderboard = false
    val disabled = itemRepository.save(
      createItem(name = "Disabled").copy(showOnLeaderboard = true, enabled = false)
    )
    val account = createAccount()
    val order = orderRepository.save(Order(accountId = account.id!!, timestamp = 1L))
    listOf(shown, hidden, disabled).forEach { item ->
      orderLineRepository.save(
        OrderLine(
          orderId = order.id,
          itemId = item.id,
          itemCount = 2,
          message = null,
          usedVoucher = false,
          paidAmount = BigInteger.valueOf(200)
        )
      )
    }
    orderLineRepository.flush()

    val leaderboard = orderLineRepository.getItemConsumptionLeaderboard(10)
    assertEquals(1, leaderboard.size)
    assertEquals("Shown", leaderboard[0].itemName)
  }

}
