package hu.bme.sch.kirpay.principal

import hu.bme.sch.kirpay.event.EventService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.UserDetailsManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional
class PrincipalAuthenticationDetailsManager(
  private val principalRepository: PrincipalRepository,
  private val eventService: EventService,
  private val clock: Clock,
  private val passwordEncoder: PasswordEncoder
) : UserDetailsManager {
  override fun loadUserByUsername(username: String): UserDetails {
    val principal = principalRepository.findByName(username)
      ?: throw UsernameNotFoundException("Felhasználó '$username' nem található!")

    return principal
  }

  override fun createUser(user: UserDetails) {
    val createdAt = clock.millis()
    val principal = Principal(
      id = null,
      name = requireNotNull(user.username) { "Username must not be null" },
      secret = passwordEncoder.encode(user.password!!)!!,
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
    eventService.logPrincipalCreated(principal, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  override fun updateUser(user: UserDetails) {
    val principal = principalRepository.findByName(user.username)
      ?: throw UsernameNotFoundException("A felhasználót nem lehet módosítani, '${user.username}' nem található!")

    val newPrincipal = principal.copy(
      name = requireNotNull(user.username) { "Username must not be null" },
      secret = passwordEncoder.encode(user.password!!)!!,
      active = user.isEnabled
    ).copyWithAuthorities(user.authorities)

    principalRepository.save(newPrincipal)
    eventService.logPrincipalUpdated(principal, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  override fun deleteUser(username: String) {
    val principal = principalRepository.findByName(username)
      ?: throw IllegalArgumentException("A felhasználót nem lehet törölni, mert nem létezik!")

    principalRepository.delete(principal)
    eventService.logPrincipalDeleted(principal, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  override fun changePassword(oldPassword: String?, newPassword: String?) {
    requireNotNull(newPassword) { "Kötelező jelszót megadni!" }
    val currentUser = SecurityContextHolder.getContextHolderStrategy().context.authentication
      ?: throw AccessDeniedException("Nem lehet módosítani a jelszavat, mivel a felhasználó nincs belépve!")

    val username = currentUser.name

    val principal = principalRepository.findByName(username)
      ?: throw UsernameNotFoundException("Nem lehet módosítani a felhasználót, '${username}' nem található!")

    principalRepository.save(principal.copy(secret = passwordEncoder.encode(newPassword)!!))
  }

  override fun userExists(username: String): Boolean {
    return principalRepository.findByName(username) != null
  }

}
