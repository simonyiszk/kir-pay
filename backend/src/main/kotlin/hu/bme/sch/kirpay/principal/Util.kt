package hu.bme.sch.kirpay.principal

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock


fun getLoggedInPrincipal(): Principal? = (SecurityContextHolder.getContext().authentication?.principal as? Principal)


fun Principal.copyWithAuthorities(authorities: Collection<GrantedAuthority>): Principal {
  val role = if (isRoleGranted(Role.ADMIN.name, authorities)) {
    Role.ADMIN
  } else if (isRoleGranted(Role.TERMINAL.name, authorities)) {
    Role.TERMINAL
  } else {
    throw IllegalArgumentException("A principal must be either a terminal or admin!")
  }

  return this.copy(
    role = role,
    canRedeemVouchers = isRoleGranted(Permission.REDEEM_VOUCHERS.name, authorities),
    canSellItems = isRoleGranted(Permission.SELL_ITEMS.name, authorities),
    canTransfer = isRoleGranted(Permission.TRANSFER_FUNDS.name, authorities),
    canUpload = isRoleGranted(Permission.UPLOAD_FUNDS.name, authorities),
    canAssignCards = isRoleGranted(Permission.ASSIGN_CARDS.name, authorities)
  )
}


fun getPrincipalAuthorities(principal: Principal): MutableCollection<out GrantedAuthority> = listOfNotNull(
  when (principal.role) {
    Role.TERMINAL -> Role.TERMINAL.name
    Role.ADMIN -> Role.ADMIN.name
  },
  if (principal.canRedeemVouchers) Permission.REDEEM_VOUCHERS.name else null,
  if (principal.canSellItems) Permission.SELL_ITEMS.name else null,
  if (principal.canTransfer) Permission.TRANSFER_FUNDS.name else null,
  if (principal.canUpload) Permission.UPLOAD_FUNDS.name else null,
  if (principal.canAssignCards) Permission.ASSIGN_CARDS.name else null,
).map { SimpleGrantedAuthority("ROLE_$it") }.toMutableList()


private fun isRoleGranted(role: String, authorities: Collection<GrantedAuthority>) =
  authorities.any { it.equals("ROLE_$role") }


fun PrincipalDto.toPrincipal(encoder: PasswordEncoder, clock: Clock): Principal {
  val createdAt = clock.millis()
  return Principal(
    id = null,
    name = name,
    secret = encoder.encode(password),
    role = role,
    active = active,
    canRedeemVouchers = canRedeemVouchers,
    canUpload = canUpload,
    canTransfer = canTransfer,
    canSellItems = canSellItems,
    canAssignCards = canAssignCards,
    createdAt = createdAt,
    lastUsed = createdAt,
  )
}
