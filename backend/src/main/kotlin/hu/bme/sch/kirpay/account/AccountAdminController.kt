package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.ADMIN_API
import hu.bme.sch.kirpay.common.CsvParserFactory
import hu.bme.sch.kirpay.common.asFileAttachment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping(ADMIN_API)
class AccountAdminController(
  parserFactory: CsvParserFactory,
  private val accountService: AccountService
) {
  private val accountParser = parserFactory.getParserForType(Account::class)


  @PostMapping("/accounts/{accountId}/disable")
  fun disableAccount(@PathVariable accountId: Int) = accountService.setEnabled(accountId, false)


  @PostMapping("/accounts/{accountId}/enable")
  fun enableAccount(@PathVariable accountId: Int) = accountService.setEnabled(accountId, true)


  @PostMapping("/accounts")
  fun createAccount(@RequestBody dto: AccountCreateDto) = accountService.create(dto)


  @PostMapping("/accounts/{accountId}")
  fun updateAccount(@PathVariable accountId: Int, @RequestBody dto: AccountUpdateDto) =
    accountService.update(accountId, dto)


  @DeleteMapping("/accounts/{accountId}")
  fun deleteAccount(@PathVariable accountId: Int) = accountService.deleteAccount(accountId)


  @PostMapping("/import/accounts", consumes = [MediaType.TEXT_PLAIN_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun importAccounts(@RequestBody csv: String) {
    val accounts = accountParser.fromCsv(csv)
    accountService.importAccounts(accounts)
  }


  @GetMapping("/export/accounts", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportAccounts(): ResponseEntity<String> {
    val accounts = accountService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("accounts.csv")
      .body(accountParser.toCsv(accounts))
  }


  @GetMapping("/template/accounts", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun accountExportTemplate(): ResponseEntity<String> {
    val accounts: List<Account> = listOf()
    return ResponseEntity.ok()
      .asFileAttachment("accounts-template.csv")
      .body(accountParser.toCsv(accounts))
  }

}
