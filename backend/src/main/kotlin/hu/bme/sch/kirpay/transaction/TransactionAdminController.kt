package hu.bme.sch.kirpay.transaction

import hu.bme.sch.kirpay.common.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(ADMIN_API)
class TransactionAdminController(
  parserFactory: CsvParserFactory,
  private val transactionService: TransactionService
) {
  private val transactionParser = parserFactory.getParserForType(Transaction::class)


  @GetMapping("/transactions")
  fun getTransactionsPaginated(@RequestParam(required = false) page: Int?, @RequestParam(required = false) size: Int?) =
    if (page == null && size == null)
      transactionService.findAll()
    else
      transactionService.findPaginated(page ?: DEFAULT_PAGE, size ?: DEFAULT_PAGE_SIZE)


  @GetMapping("/export/transactions", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportTransactions(): ResponseEntity<String> {
    val transactions = transactionService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("transactions.csv")
      .body(transactionParser.toCsv(transactions))
  }

}
