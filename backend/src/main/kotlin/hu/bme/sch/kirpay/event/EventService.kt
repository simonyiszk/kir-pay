package hu.bme.sch.kirpay.event

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.order.Item
import hu.bme.sch.kirpay.order.Voucher
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRef
import hu.bme.sch.kirpay.principal.Role
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class EventService(private val eventRepository: EventRepository) {
  fun save(event: Event) = eventRepository.save(event)

  fun findAll() = eventRepository.findAllOrderByTimestampDesc().toList()

  fun findPaginated(page: Int, pageSize: Int) =
    eventRepository.findAllOrderByTimestampDescPaginated(page.toLong() * pageSize, pageSize)

  fun create(event: String, message: String, performedBy: String, timestamp: Long) {
    eventRepository.save(
      Event(
        id = null,
        event = event,
        timestamp = timestamp,
        message = message,
        performedBy = performedBy
      )
    )
  }

  fun formatPerformerPrincipal(by: PrincipalRef?): String = by?.name ?: "Ismeretlen végrehajtó"

  fun displayPrincipal(principal: Principal) =
    "${principal.name} | ${
      when (principal.role) {
        Role.ADMIN -> "Adminisztrátor"
        Role.TERMINAL -> "Terminál"
      }
    }"

  private fun displayAccount(account: Account) =
    "${account.id}: " + account.name + (account.email?.let { " - $it" } ?: "")

  private fun displayItem(item: Item) =
    "${item.name}: ${item.stock} db, ${item.cost} JMF - ${if (item.enabled) "Aktív" else "Inaktív"}"

  private fun displayVoucher(voucher: Voucher) =
    "Számlaazonosító: ${voucher.accountId}, Termékazonosító: ${voucher.itemId}, Darabszám: ${voucher.count}"

  fun logAccountCreated(account: Account, by: PrincipalRef?, timestamp: Long) =
    create("Számla létrehozva", displayAccount(account), formatPerformerPrincipal(by), timestamp)

  fun logAccountCardAssigned(account: Account, by: PrincipalRef?, timestamp: Long) =
    create("Kártya hozzárendelve", displayAccount(account), formatPerformerPrincipal(by), timestamp)

  fun logAccountUpdated(account: Account, by: PrincipalRef?, timestamp: Long) =
    create("Számla szerkesztve", displayAccount(account), formatPerformerPrincipal(by), timestamp)

  fun logAccountDeleted(account: Account, by: PrincipalRef?, timestamp: Long) =
    create("Számla törölve", displayAccount(account), formatPerformerPrincipal(by), timestamp)

  fun logPay(account: Account, amount: Long, by: PrincipalRef?, timestamp: Long) =
    create("Fizetés", "${displayAccount(account)} | $amount JMF", formatPerformerPrincipal(by), timestamp)

  fun logUpload(account: Account, amount: Long, by: PrincipalRef?, timestamp: Long) =
    create("Feltöltés", "${displayAccount(account)} | $amount JMF", formatPerformerPrincipal(by), timestamp)

  fun logBalanceTransfer(sender: Account, recipient: Account, amount: Long, by: PrincipalRef?, timestamp: Long) =
    create(
      "Átutalás",
      "${displayAccount(sender)} -> ${displayAccount(recipient)} | $amount JMF",
      formatPerformerPrincipal(by),
      timestamp
    )

  fun logOrderCreated(orderId: Int?, accountId: Int, by: PrincipalRef?, timestamp: Long) =
    create(
      "Rendelés létrehozva",
      "Rendelésazonosító: $orderId - Számlaazonosító: $accountId",
      formatPerformerPrincipal(by),
      timestamp
    )

  fun logItemSold(
    orderId: Int?,
    accountId: Int?,
    item: String?,
    message: String?,
    amount: Long,
    count: Int,
    by: PrincipalRef?,
    timestamp: Long
  ) {
    val itemPart = if (item != null) ", Termék: $item" else ""
    val messagePart = message?.let { ", Üzenet: $it" } ?: ""
    val saleNumbers = "Mennyiség: $count, Fizetve: $amount"
    create(
      "Termék eladva",
      "Rendelésazonosító: $orderId - Számlaazonosító: $accountId | $saleNumbers$itemPart$messagePart",
      formatPerformerPrincipal(by),
      timestamp
    )
  }

  fun logVoucherRedeemed(
    orderId: Int?,
    accountId: Int?,
    item: String,
    message: String?,
    count: Int,
    by: PrincipalRef?,
    timestamp: Long
  ) {
    val itemPart = ", Termék: $item"
    val messagePart = message?.let { ", Üzenet: $it" } ?: ""
    val countMsg = "Mennyiség: $count"
    create(
      "Utalvány beváltva",
      "Rendelésazonosító: $orderId - Számlaazonosító: $accountId | $countMsg$itemPart$messagePart",
      formatPerformerPrincipal(by),
      timestamp
    )
  }

  fun logItemCreated(item: Item, by: PrincipalRef?, timestamp: Long) =
    create("Termék létrehozva", displayItem(item), formatPerformerPrincipal(by), timestamp)

  fun logItemUpdated(item: Item, by: PrincipalRef?, timestamp: Long) =
    create("Termék módosítva", displayItem(item), formatPerformerPrincipal(by), timestamp)

  fun logItemDeleted(item: Item, by: PrincipalRef?, timestamp: Long) =
    create("Termék törölve", displayItem(item), formatPerformerPrincipal(by), timestamp)

  fun logVoucherCreated(voucher: Voucher, by: PrincipalRef?, timestamp: Long) =
    create("Utalvány Létrehozva", displayVoucher(voucher), formatPerformerPrincipal(by), timestamp)

  fun logVoucherUpdated(voucher: Voucher, by: PrincipalRef?, timestamp: Long) =
    create("Utalvány módosítva", displayVoucher(voucher), formatPerformerPrincipal(by), timestamp)

  fun logVoucherDeleted(voucher: Voucher, by: PrincipalRef?, timestamp: Long) =
    create("Utalvány törölve", displayVoucher(voucher), formatPerformerPrincipal(by), timestamp)

  fun logPrincipalCreated(who: Principal, by: PrincipalRef?, timestamp: Long) =
    create("Principal létrehozva", displayPrincipal(who), formatPerformerPrincipal(by), timestamp)

  fun logPrincipalUpdated(who: Principal, by: PrincipalRef?, timestamp: Long) =
    create("Principal módosítva", displayPrincipal(who), formatPerformerPrincipal(by), timestamp)

  fun logPrincipalDeleted(who: Principal, by: PrincipalRef?, timestamp: Long) =
    create("Principal törölve", displayPrincipal(who), formatPerformerPrincipal(by), timestamp)

}
