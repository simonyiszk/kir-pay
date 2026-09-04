package hu.bme.sch.kirpay.app

import hu.bme.sch.kirpay.account.AccountService
import hu.bme.sch.kirpay.common.ADMIN_API
import hu.bme.sch.kirpay.transaction.RevenueHeatmapEntry
import hu.bme.sch.kirpay.transaction.TransactionService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ADMIN_API)
class AnalyticsAdminController(
  private val accountService: AccountService,
  private val transactionService: TransactionService
) {
  data class AnalyticsDto(
    val accountCount: Long,
    val transactionCount: Long,
    val allActiveBalance: java.math.BigInteger,
    val income: java.math.BigInteger,
    val allUploads: java.math.BigInteger,
    val transactionVolume: java.math.BigInteger,
  )

  @Transactional(readOnly = true)
  @GetMapping("/analytics")
  fun getAnalytics() = AnalyticsDto(
    accountCount = accountService.countAll(),
    transactionCount = transactionService.countAll(),
    allActiveBalance = accountService.getAllActiveBalance(),
    income = transactionService.getIncome(),
    allUploads = transactionService.getAllUploads(),
    transactionVolume = transactionService.getTransactionVolume()
  )

  @Transactional(readOnly = true)
  @GetMapping("/analytics/revenue-heatmap")
  fun getRevenueHeatmap(): List<RevenueHeatmapEntry> = transactionService.revenueHeatmap()

}
