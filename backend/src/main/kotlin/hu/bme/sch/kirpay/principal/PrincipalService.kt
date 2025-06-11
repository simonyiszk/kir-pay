package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.common.BadRequestException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock


@Service
@Transactional
class PrincipalService(
  private val principalRepository: PrincipalRepository,
  private val events: ApplicationEventPublisher,
  private val passwordEncoder: PasswordEncoder,
  private val clock: Clock
) {

  fun findAll(): List<Principal> = principalRepository.findAllOrderByName()


  fun find(id: Int): Principal = principalRepository.findById(id)
    .orElseThrow { BadRequestException("A principal nem létezik!") }


  fun createPrincipal(principal: PrincipalDto, failOnCollision: Boolean = true): Principal? {
    if (principalRepository.findByName(principal.name) != null) {
      if (!failOnCollision) return null
      throw BadRequestException("Már létezik principal ezzel a felhasználónévvel!")
    }
    val importedPrincipal = principal.toPrincipal(passwordEncoder, clock)


    events.publishEvent(PrincipalCreatedEvent(importedPrincipal, getLoggedInPrincipal(), clock.millis()))
    return principalRepository.save(importedPrincipal)
  }


  fun importPrincipals(principals: List<PrincipalDto>) = principals.forEach { createPrincipal(it) }


  fun updatePrincipal(id: Int, dto: PrincipalDto): Principal {
    val thisPrincipal = getLoggedInPrincipal()
    val principal = find(id)
    if (principal.role == Role.ADMIN) {
      if (dto.role != Role.ADMIN && id == thisPrincipal?.id) throw BadRequestException("Ne zárd ki magad!")
      if (!dto.active) throw BadRequestException("Adminokat nem lehet letiltani!")
    }

    val updated = dto.toPrincipal(passwordEncoder, clock).copy(id = id)
    val secret = if (dto.password == "***") principal.secret else updated.secret
    val newPrincipal = principalRepository.save(updated.copy(secret = secret))

    events.publishEvent(PrincipalUpdatedEvent(updated, getLoggedInPrincipal(), clock.millis()))
    return newPrincipal
  }


  fun delete(principalId: Int) {
    val principal = find(principalId)
    if (principal.role == Role.ADMIN) throw BadRequestException("Adminokat nem lehet törölni!")
    principalRepository.delete(principal)
    events.publishEvent(PrincipalDeletedEvent(principal, getLoggedInPrincipal(), clock.millis()))
  }


  fun setEnabled(id: Int, enabled: Boolean): Principal {
    val principal = find(id)
    if (principal.role == Role.ADMIN && !enabled) throw BadRequestException("Admint nem lehet letiltani")
    val newPrincipal = principalRepository.save(principal.copy(active = enabled))
    events.publishEvent(PrincipalUpdatedEvent(newPrincipal, getLoggedInPrincipal(), clock.millis()))
    return newPrincipal
  }


  fun updateLastUsed(id: Int?) {
    if (id == null) return
    principalRepository.findById(id).ifPresent { principalRepository.save(it.copy(lastUsed = clock.millis())) }
  }

}
