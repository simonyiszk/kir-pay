package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.common.*
import hu.bme.sch.kirpay.event.EventService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.*

@Service
@Transactional(readOnly = true)
class PrincipalService(
  private val principalRepository: PrincipalRepository,
  private val eventService: EventService,
  private val passwordEncoder: PasswordEncoder,
  private val clock: Clock,
  private val idempotencyService: IdempotencyService
) {
  fun findAll(): List<Principal> = principalRepository.findAllOrderByName()

  fun find(id: Int): Principal = principalRepository.findById(id)
    .orElseThrow { BadRequestException("A principal nem létezik!") }

  @RetryTransaction
  @Transactional
  fun createPrincipal(dto: PrincipalAdminController.PrincipalCreateDto): Principal = idempotencyService.execute(
    dto.idempotencyKey,
    IdempotentOperationType.PRINCIPAL_CREATE,
    buildFingerprint(IdempotentOperationType.PRINCIPAL_CREATE,
      dto.name, dto.role, dto.active,
      dto.canUpload, dto.canTransfer, dto.canSellItems, dto.canRedeemVouchers, dto.canAssignCards),
    Principal::class
  ) {
    createPrincipal(dto.toPrincipalDto(), failOnCollision = true)
      ?: throw InternalErrorException("A principal létrehozása sikertelen!")
  }.value

  @Transactional
  fun createPrincipal(principal: PrincipalDto, failOnCollision: Boolean = true): Principal? {
    val existing = principalRepository.findByName(principal.name)
    if (existing != null) {
      if (!failOnCollision) {
        val secret =
          if (principal.password == "***") existing.secret else principal.toPrincipal(passwordEncoder, clock).secret
        val saved = principalRepository.save(
          existing.copy(
            secret = secret,
            role = principal.role,
            active = principal.active,
            canUpload = principal.canUpload,
            canTransfer = principal.canTransfer,
            canSellItems = principal.canSellItems,
            canRedeemVouchers = principal.canRedeemVouchers,
            canAssignCards = principal.canAssignCards
          )
        )
        eventService.logPrincipalUpdated(saved, null, clock.millis())
        return saved
      }
      throw BadRequestException("Már létezik principal ezzel a felhasználónévvel!")
    }
    val importedPrincipal = principal.toPrincipal(passwordEncoder, clock)
    try {
      val saved = principalRepository.save(importedPrincipal)
      eventService.logPrincipalCreated(saved, getLoggedInPrincipal()?.toRef(), clock.millis())
      return saved
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("Már létezik principal ezzel a felhasználónévvel!")
    }
  }

  @RetryTransaction
  @Transactional
  fun importPrincipals(principals: List<PrincipalDto>, idempotencyKey: UUID, csv: String): BulkResult =
    idempotencyService.execute(
      idempotencyKey,
      IdempotentOperationType.PRINCIPAL_IMPORT,
      buildFingerprint(IdempotentOperationType.PRINCIPAL_IMPORT, csv),
      BulkResult::class
    ) {
      principals.forEach { createPrincipal(it) }
      BulkResult(principals.size)
    }.value

  @Transactional
  fun updatePrincipal(id: Int, dto: PrincipalDto): Principal {
    val thisPrincipal = getLoggedInPrincipal()
    val principal = find(id)
    if (principal.role == Role.ADMIN) {
      if (dto.role != Role.ADMIN && id == thisPrincipal?.id) throw BadRequestException("Ne zárd ki magad!")
      if (!dto.active) throw BadRequestException("Adminokat nem lehet letiltani!")
    }

    if (dto.name != principal.name && principalRepository.findByName(dto.name) != null) {
      throw BadRequestException("Már létezik principal ezzel a felhasználónévvel!")
    }

    val updated = dto.toPrincipal(passwordEncoder, clock).copy(id = id)
    val secret = if (dto.password == "***") principal.secret else updated.secret
    val newPrincipal = principalRepository.save(updated.copy(secret = secret,
      createdAt = principal.createdAt,
      lastUsed = principal.lastUsed,
      version = principal.version))

    eventService.logPrincipalUpdated(newPrincipal, getLoggedInPrincipal()?.toRef(), clock.millis())
    return newPrincipal
  }

  @Transactional
  fun delete(principalId: Int) {
    val principal = find(principalId)
    if (principal.role == Role.ADMIN) throw BadRequestException("Adminokat nem lehet törölni!")
    principalRepository.delete(principal)
    eventService.logPrincipalDeleted(principal, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  @Transactional
  fun setEnabled(id: Int, enabled: Boolean): Principal {
    val principal = find(id)
    if (principal.role == Role.ADMIN && !enabled) throw BadRequestException("Admint nem lehet letiltani")
    val newPrincipal = principalRepository.save(principal.copy(active = enabled))
    eventService.logPrincipalUpdated(newPrincipal, getLoggedInPrincipal()?.toRef(), clock.millis())
    return newPrincipal
  }

  @Transactional
  fun updateLastUsed(id: Int?) {
    if (id == null) return
    principalRepository.updateLastUsed(id, clock.millis())
  }

}
