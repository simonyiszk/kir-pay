package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.common.ADMIN_API
import hu.bme.sch.kirpay.common.CsvParserFactory
import hu.bme.sch.kirpay.common.asFileAttachment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(ADMIN_API)
class PrincipalAdminController(
  parserFactory: CsvParserFactory,
  private val principalService: PrincipalService
) {
  private val principalParser = parserFactory.getParserForType(Principal::class)
  private val principalDtoParser = parserFactory.getParserForType(PrincipalDto::class)

  @GetMapping("/principals")
  fun findAll() = principalService.findAll()

  @PostMapping("/principals/{principalId}/disable")
  fun disablePrincipal(@PathVariable principalId: Int) = principalService.setEnabled(principalId, false)

  @PostMapping("/principals/{principalId}/enable")
  fun enablePrincipal(@PathVariable principalId: Int) = principalService.setEnabled(principalId, true)

  data class PrincipalCreateDto(
    val name: String,
    val password: String,
    val role: Role,
    val canUpload: Boolean,
    val canTransfer: Boolean,
    val canSellItems: Boolean,
    val canRedeemVouchers: Boolean,
    val canAssignCards: Boolean,
    val active: Boolean,
    val idempotencyKey: UUID
  ) {
    fun toPrincipalDto() = PrincipalDto(
      name = name,
      password = password,
      role = role,
      canUpload = canUpload,
      canTransfer = canTransfer,
      canSellItems = canSellItems,
      canRedeemVouchers = canRedeemVouchers,
      canAssignCards = canAssignCards,
      active = active
    )
  }

  @PostMapping("/principals")
  fun createPrincipal(@RequestBody dto: PrincipalCreateDto) = principalService.createPrincipal(dto)

  @PostMapping("/principals/{principalId}")
  fun updatePrincipal(
    @PathVariable principalId: Int,
    @RequestBody dto: PrincipalDto
  ) = principalService.updatePrincipal(principalId, dto)

  @DeleteMapping("/principals/{principalId}")
  fun deletePrincipal(@PathVariable principalId: Int) = principalService.delete(principalId)

  @GetMapping("/export/principals", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun exportPrincipals(): ResponseEntity<String> {
    val orderLines = principalService.findAll()
    return ResponseEntity.ok()
      .asFileAttachment("principals.csv")
      .body(principalParser.toCsv(orderLines))
  }

  @PostMapping("/import/principals", consumes = [MediaType.TEXT_PLAIN_VALUE])
  @ResponseStatus(HttpStatus.CREATED)
  fun importPrincipals(@RequestBody csv: String, @RequestParam idempotencyKey: UUID) {
    val principals = principalDtoParser.fromCsv(csv)
    principalService.importPrincipals(principals, idempotencyKey, csv)
  }

  @GetMapping("/template/principals", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
  fun principalExportTemplate(): ResponseEntity<String> {
    val principals: List<PrincipalDto> = listOf()
    return ResponseEntity.ok()
      .asFileAttachment("principals-template.csv")
      .body(principalDtoParser.toCsv(principals))
  }

}
