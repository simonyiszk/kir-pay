package hu.bme.sch.kirpay.principal

import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.provisioning.UserDetailsManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


@Service
@Transactional
@OptIn(ExperimentalContracts::class)
class PrincipalAuthenticationDetailsManager(
  private val principalRepository: PrincipalRepository,
  private val events: ApplicationEventPublisher,
  private val clock: Clock
) : UserDetailsManager {

  override fun loadUserByUsername(username: String?): UserDetails {
    validateText(username, "A felhasználó kereséshez nem lett felhasználónév megadva")
    val principal = principalRepository.findByName(username)
      ?: throw UsernameNotFoundException("Felhasználó '$username' nem található!")

    events.publishEvent(PrincipalAuthenticatedEvent(principal, clock.millis()))
    return principal
  }


  override fun createUser(user: UserDetails?) {
    validateUser(user, "A felhasználót nem lehet létrehozni, az adatok formátuma helytelen!")

    val createdAt = clock.millis()
    val principal = Principal(
      id = null,
      name = user.username,
      secret = user.password,
      active = user.isEnabled,
      role = Role.TERMINAL,
      canRedeemVouchers = false,
      canSellItems = false,
      canTransfer = false,
      canUpload = false,
      canAssignCards = false,
      createdAt = createdAt,
      lastUsed = createdAt
    ).copyWithAuthorities(user.authorities)

    principalRepository.save(principal)
    events.publishEvent(PrincipalCreatedEvent(principal, getLoggedInPrincipal(), clock.millis()))
  }


  override fun updateUser(user: UserDetails?) {
    validateUser(user, "A felhasználót nem lehet módosítani, az adatok formátuma helytelen!")
    val principal = principalRepository.findByName(user.username)
      ?: throw UsernameNotFoundException("A felhasználót nem lehet módosítani, '${user.username}' nem található!")

    val newPrincipal = principal.copy(
      name = user.username,
      secret = user.password,
      active = user.isEnabled
    ).copyWithAuthorities(user.authorities)

    principalRepository.save(newPrincipal)
    events.publishEvent(PrincipalUpdatedEvent(principal, getLoggedInPrincipal(), clock.millis()))
  }


  override fun deleteUser(username: String?) {
    validateText(username, "A felhasználó törléséhez nem lett felhasználónév megadva!")
    val principal = principalRepository.findByName(username)
      ?: throw IllegalArgumentException("A felhasználót nem lehet törölni, mert nem létezik!")

    principalRepository.delete(principal)
    events.publishEvent(PrincipalDeletedEvent(principal, getLoggedInPrincipal(), clock.millis()))
  }


  override fun changePassword(oldPassword: String?, newPassword: String?) {
    validateText(newPassword, "Nincs megadva jelszó a módosításhoz!")
    val currentUser = SecurityContextHolder.getContextHolderStrategy().context.authentication
      ?: throw AccessDeniedException("Nem lehet módosítani a jelszavat, mivel a felhasználó nincs belépve!")

    val username = currentUser.name
    validateText(username, "A jelszó módosításhoz nem lett felhasználónév megadva!")

    val principal = principalRepository.findByName(username)
      ?: throw UsernameNotFoundException("Nem lehet módosítani a felhasználót, '${username}' nem található!")

    principalRepository.save(principal.copy(secret = newPassword))
  }


  override fun userExists(username: String?): Boolean {
    if (username.isNullOrBlank()) return false
    return principalRepository.findByName(username) != null
  }


  private final fun validateText(username: String?, message: String) {
    contract {
      returns() implies (username != null)
    }
    if (username.isNullOrBlank()) throw IllegalArgumentException(message)
  }


  private final fun validateUser(user: UserDetails?, message: String) {
    contract {
      returns() implies (user != null)
    }
    if (user == null) throw IllegalArgumentException(message)
    validateText(user.username, message)
    validateText(user.password, message)

    if (user.authorities == null) throw IllegalArgumentException(message)
    if (user.authorities.any { it == null || it.authority.isNullOrBlank() }) throw IllegalArgumentException(message)
  }

}
