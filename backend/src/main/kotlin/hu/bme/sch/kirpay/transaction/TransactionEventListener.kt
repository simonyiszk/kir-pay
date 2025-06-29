package hu.bme.sch.kirpay.transaction

import hu.bme.sch.kirpay.account.AccountBalanceTransferEvent
import hu.bme.sch.kirpay.account.AccountPayEvent
import hu.bme.sch.kirpay.account.AccountUploadEvent
import hu.bme.sch.kirpay.order.ItemSoldEvent
import org.springframework.context.annotation.Configuration
import org.springframework.modulith.events.ApplicationModuleListener

@Configuration
class TransactionEventListener(private val transactionRepository: TransactionRepository) {

  @ApplicationModuleListener
  fun on(event: AccountPayEvent) {
    transactionRepository.save(
      Transaction(
        id = null,
        type = TransactionType.CHARGE,
        senderId = event.account.id,
        recipientId = null,
        amount = event.amount,
        message = null,
        timestamp = event.timestamp
      )
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountUploadEvent) {
    transactionRepository.save(
      Transaction(
        id = null,
        type = TransactionType.TOP_UP,
        recipientId = event.account.id,
        senderId = null,
        amount = event.amount,
        message = null,
        timestamp = event.timestamp
      )
    )
  }


  @ApplicationModuleListener
  fun on(event: AccountBalanceTransferEvent) {
    transactionRepository.save(
      Transaction(
        id = null,
        type = TransactionType.TRANSFER,
        senderId = event.sender.id,
        recipientId = event.recipient.id,
        amount = event.amount,
        message = null,
        timestamp = event.timestamp
      )
    )
  }


  @ApplicationModuleListener
  fun on(event: ItemSoldEvent) {
    transactionRepository.save(
      Transaction(
        id = null,
        type = TransactionType.CHARGE,
        senderId = event.accountId,
        recipientId = null,
        amount = event.amount,
        message = getPurchaseMessage(event),
        timestamp = event.timestamp
      )
    )
  }


  private fun getPurchaseMessage(event: ItemSoldEvent): String? {
    if (event.message != null && event.item != null) return "${event.item}: ${event.message}"
    return event.message ?: event.item
  }

}
