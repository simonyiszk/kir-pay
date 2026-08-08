package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.TERMINAL_API
import hu.bme.sch.kirpay.order.VoucherService
import hu.bme.sch.kirpay.principal.PermissionName
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(TERMINAL_API)
class AccountTerminalController(
  private val accountService: AccountService,
  private val balanceService: AccountBalanceService,
  private val voucherService: VoucherService
) {
  @GetMapping("/accounts")
  @Secured(PermissionName.ASSIGN_CARDS)
  fun getAllAccounts() = accountService.findAllActive()

  @GetMapping("/accounts/{accountId}")
  @Secured(PermissionName.ASSIGN_CARDS)
  fun getAccount(@PathVariable accountId: Int) = accountService.findActive(accountId)

  data class BalanceAmountDto(
    @field:Min(1) val amount: Long,
    @field:jakarta.validation.constraints.NotNull val idempotencyKey: java.util.UUID
  )

  @PostMapping("/account-by-card/{card}/pay")
  @Secured(PermissionName.SELL_ITEMS)
  fun pay(
    @PathVariable card: String,
    @Valid @RequestBody dto: BalanceAmountDto
  ): Map<String, Any?> {
    val result = balanceService.pay(card, dto.amount, dto.idempotencyKey, logEvent = true)
    return mapAccountResponse(result.account)
  }

  @PostMapping("/account-by-card/{card}/upload")
  @Secured(PermissionName.UPLOAD_FUNDS)
  fun upload(
    @PathVariable card: String,
    @Valid @RequestBody dto: BalanceAmountDto
  ): Map<String, Any?> {
    val result = balanceService.upload(card, dto.amount, dto.idempotencyKey)
    return mapAccountResponse(result.account)
  }

  data class BalanceTransferDto(
    @field:NotBlank val recipientCard: String,
    @field:Min(1) val amount: Long,
    @field:jakarta.validation.constraints.NotNull val idempotencyKey: java.util.UUID
  )

  @PostMapping("/account-by-card/{card}/transfer")
  @Secured(PermissionName.TRANSFER_FUNDS)
  fun transfer(
    @PathVariable card: String,
    @Valid @RequestBody dto: BalanceTransferDto
  ): Map<String, Any?> {
    val result = balanceService.transfer(card, dto.recipientCard, dto.amount, dto.idempotencyKey)
    return mapAccountResponse(result.account)
  }

  data class CardAssignDto(@field:NotBlank val card: String)

  @PostMapping("/accounts/{accountId}/card")
  @Secured(PermissionName.ASSIGN_CARDS)
  fun assignCard(
    @Valid @RequestBody dto: CardAssignDto,
    @PathVariable accountId: Int
  ) = accountService.assignCard(accountId, dto.card)

  @GetMapping("/account-by-card/{card}")
  fun getCardAccount(@PathVariable card: String) =
    voucherService.getVouchersWithAccount(accountService.findActiveByCard(card))

  @GetMapping("/account-by-email/{email}")
  @Secured(PermissionName.SELL_ITEMS)
  fun getEmailAccount(@PathVariable email: String) =
    voucherService.getVouchersWithAccount(accountService.findActiveByEmail(email))

  private fun mapAccountResponse(account: Account): Map<String, Any?> =
    mapOf(
      "id" to account.id,
      "name" to account.name,
      "email" to account.email,
      "phone" to account.phone,
      "card" to account.card,
      "balance" to account.balance,
      "active" to account.active,
      "version" to account.version,
    )

}
