package hu.bme.sch.kirpay.app

import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.Role

/**
 * DTO for Principal data returned to API clients.
 * Excludes sensitive fields like [Principal.secret].
 */
data class PrincipalResponse(
    val id: Int?,
    val name: String,
    val role: Role,
    val active: Boolean,
    val canUpload: Boolean,
    val canTransfer: Boolean,
    val canSellItems: Boolean,
    val canRedeemVouchers: Boolean,
    val canAssignCards: Boolean,
    val createdAt: Long,
    val lastUsed: Long,
) {
    companion object {
        fun fromPrincipal(p: Principal) = PrincipalResponse(
            id = p.id,
            name = p.name,
            role = p.role,
            active = p.active,
            canUpload = p.canUpload,
            canTransfer = p.canTransfer,
            canSellItems = p.canSellItems,
            canRedeemVouchers = p.canRedeemVouchers,
            canAssignCards = p.canAssignCards,
            createdAt = p.createdAt,
            lastUsed = p.lastUsed,
        )
    }
}
