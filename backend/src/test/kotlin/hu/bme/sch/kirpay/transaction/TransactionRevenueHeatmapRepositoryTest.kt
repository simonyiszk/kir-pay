package hu.bme.sch.kirpay.transaction

import hu.bme.sch.kirpay.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class TransactionRevenueHeatmapRepositoryTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var transactionRepository: TransactionRepository

  private val zone = "Europe/Budapest"

  /** Local-midnight window start, exactly like TransactionService computes it. */
  private fun windowStart(localFirstDay: LocalDate): Long =
    localFirstDay.atStartOfDay(ZoneId.of(zone)).toInstant().toEpochMilli()

  private fun charge(amount: Long, instant: Instant, fingerprint: String, senderId: Int? = null) {
    transactionRepository.saveIgnoreDuplicate(
      type = "CHARGE",
      senderId = senderId,
      recipientId = null,
      amount = BigInteger.valueOf(amount),
      message = null,
      timestamp = instant.toEpochMilli(),
      fingerprint = fingerprint
    )
  }

  private fun assertGridShape(heatmap: List<RevenueHeatmapEntry>) {
    assertEquals(168, heatmap.size)
    assertEquals(168, heatmap.map { "${it.date}:${it.hour}" }.toSet().size)
    assertEquals(7, heatmap.map { it.date }.toSet().size)
  }

  private fun assertNonZeroCells(heatmap: List<RevenueHeatmapEntry>, vararg expected: Triple<String, Int, BigDecimal>) {
    assertEquals(
      expected.map { (it.first to it.second) to it.third }.toSet(),
      heatmap.filter { it.revenue != BigDecimal.ZERO }.map { (it.date to it.hour) to it.revenue }.toSet()
    )
  }

  @Test
  fun `charge lands in the correct local day-hour bucket in Europe Budapest`() {
    // window: 2024-01-01 .. 2024-01-07 (local midnights)
    val start = windowStart(LocalDate.of(2024, 1, 1))
    charge(400, Instant.parse("2024-01-01T08:30:00Z"), "hm-1") // 09:30 CET -> hour 9
    charge(100, Instant.parse("2024-01-01T08:31:00Z"), "hm-2") // same cell
    charge(300, Instant.parse("2024-01-07T20:00:00Z"), "hm-3") // 21:00 CET, still Jan 7 local (NOT 23:45Z - that spills to Jan 8!)
    // boundary: exactly the local midnight instant -> hour 0 of the first day
    charge(200, Instant.ofEpochMilli(start), "hm-4")

    val heatmap = transactionRepository.findRevenueHeatmap(start, 7, zone)

    assertGridShape(heatmap)
    // rows are ordered by date desc: index 0 is 2024-01-07, index 6 is 2024-01-01
    assertEquals("2024-01-07", heatmap[0].date)
    assertNonZeroCells(
      heatmap,
      Triple("2024-01-07", 21, BigDecimal.valueOf(300)),
      Triple("2024-01-01", 9, BigDecimal.valueOf(500)),
      Triple("2024-01-01", 0, BigDecimal.valueOf(200))
    )
  }

  @Test
  fun `dst spring forward - 2024-03-31 hour 2 does not exist`() {
    // window: 2024-03-25 .. 2024-03-31; Budapest jumps 02:00 CET -> 03:00 CEST at 01:00Z
    val start = windowStart(LocalDate.of(2024, 3, 25))
    charge(400, Instant.parse("2024-03-31T01:30:00Z"), "hm-dst-sp1") // 03:30 CEST -> hour 3

    val heatmap = transactionRepository.findRevenueHeatmap(start, 7, zone)

    assertGridShape(heatmap)
    assertEquals("2024-03-31", heatmap[0].date)
    assertNonZeroCells(heatmap, Triple("2024-03-31", 3, BigDecimal.valueOf(400))) // hour 2 stays 0
  }

  @Test
  fun `dst fall back - 2024-10-27 local hour 2 occurs twice and sums`() {
    // window: 2024-10-21 .. 2024-10-27; Budapest falls 03:00 CEST -> 02:00 CET at 01:00Z
    val start = windowStart(LocalDate.of(2024, 10, 21))
    charge(150, Instant.parse("2024-10-27T00:30:00Z"), "hm-dst-fb1") // 02:30 CEST
    charge(250, Instant.parse("2024-10-27T01:30:00Z"), "hm-dst-fb2") // 02:30 CET

    val heatmap = transactionRepository.findRevenueHeatmap(start, 7, zone)

    assertGridShape(heatmap)
    assertEquals("2024-10-27", heatmap[0].date)
    assertNonZeroCells(heatmap, Triple("2024-10-27", 2, BigDecimal.valueOf(400)))
  }

  @Test
  fun `charges before the window are excluded - union grid still yields exactly 168 rows`() {
    val start = windowStart(LocalDate.of(2024, 1, 1)) // = 2023-12-31T23:00:00Z
    // 2023-12-31T22:59:59Z is 23:59:59 CET on 2023-12-31: one millisecond before the window
    charge(100, Instant.parse("2023-12-31T22:59:59Z"), "hm-out")

    val heatmap = transactionRepository.findRevenueHeatmap(start, 7, zone)

    assertGridShape(heatmap) // would exceed 168 rows / 7 dates if the WHERE guard were dropped
    assertNonZeroCells(heatmap)
  }

  @Test
  fun `revenue survives account deletion - CHARGE sender is set null not cascaded`() {
    val start = windowStart(LocalDate.of(2024, 1, 1))
    val account = createAccount(balance = 0) // safeDelete requires balance 0
    charge(250, Instant.parse("2024-01-02T12:00:00Z"), "hm-del-1", senderId = account.id!!) // 13:00 CET

    val deleted = accountRepository.safeDelete(account.id!!, account.version) // version is 0
    assertEquals(1, deleted)

    val heatmap = transactionRepository.findRevenueHeatmap(start, 7, zone)

    assertGridShape(heatmap)
    assertNonZeroCells(heatmap, Triple("2024-01-02", 13, BigDecimal.valueOf(250)))
    // direct evidence that the FK action was SET NULL, not CASCADE
    val rows = transactionRepository.findAllOrderByTimestampDesc().filter { it.fingerprint == "hm-del-1" }
    assertEquals(1, rows.size)
    assertEquals(null, rows.single().senderId)
  }

}
