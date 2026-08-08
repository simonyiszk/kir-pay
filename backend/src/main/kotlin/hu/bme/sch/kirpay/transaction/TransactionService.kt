package hu.bme.sch.kirpay.transaction

import hu.bme.sch.kirpay.common.buildFingerprint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger

@Service
@Transactional
class TransactionService(private val transactionRepository: TransactionRepository) {
  private val logger = LoggerFactory.getLogger(TransactionService::class.java)

  fun findAll() = transactionRepository.findAllOrderByTimestampDesc()

  fun findPaginated(page: Int, size: Int) =
    transactionRepository.findAllOrderByTimestampDescPaginated(page.toLong() * size, size)

  fun countAll() = transactionRepository.count()

  fun getIncome(): BigInteger = transactionRepository.getIncome()

  fun getTransactionVolume(): BigInteger = transactionRepository.getTransactionVolume()

  fun getAllUploads(): BigInteger = transactionRepository.getAllUploads()

  fun recordPay(accountId: Int, amount: Long, timestamp: Long) {
    assert(amount > 0) { "recordPay amount must be positive, got $amount" }
    val fingerprint = buildFingerprint("PAY", accountId, amount, timestamp)
    saveWithFingerprint(TransactionType.CHARGE.name, accountId, null, amount, null, timestamp, fingerprint)
  }

  fun recordUpload(accountId: Int, amount: Long, timestamp: Long) {
    assert(amount > 0) { "recordUpload amount must be positive, got $amount" }
    val fingerprint = buildFingerprint("UPLOAD", accountId, amount, timestamp)
    saveWithFingerprint(TransactionType.TOP_UP.name, null, accountId, amount, null, timestamp, fingerprint)
  }

  fun recordTransfer(senderId: Int, recipientId: Int, amount: Long, timestamp: Long) {
    assert(amount > 0) { "recordTransfer amount must be positive, got $amount" }
    assert(senderId != recipientId) { "Transfer sender and recipient must differ: $senderId" }
    val fingerprint = buildFingerprint("TRANSFER", senderId, recipientId, amount, timestamp)
    saveWithFingerprint(TransactionType.TRANSFER.name, senderId, recipientId, amount, null, timestamp, fingerprint)
  }

  fun recordItemSold(
    orderId: Int?,
    orderLineId: Int?,
    accountId: Int?,
    item: String?,
    message: String?,
    amount: Long,
    count: Int,
    timestamp: Long
  ) {
    if (amount == 0L) {
      logger.info("Skipping Transaction insert for zero-amount item sale (order: $orderId, account: $accountId, item: $item)")
      return
    }
    val fingerprint = buildFingerprint("SELL", orderId, orderLineId, accountId, item, amount, count)
    saveWithFingerprint(
      TransactionType.CHARGE.name,
      accountId,
      null,
      amount,
      getPurchaseMessage(item, message),
      timestamp,
      fingerprint
    )
  }

  private fun getPurchaseMessage(item: String?, message: String?): String? {
    if (message != null && item != null) return "$item: $message"
    return message ?: item
  }

  private fun saveWithFingerprint(
    type: String,
    senderId: Int?,
    recipientId: Int?,
    amount: Long,
    message: String?,
    timestamp: Long,
    fingerprint: String
  ) {
    val inserted = transactionRepository.saveIgnoreDuplicate(
      type = type,
      senderId = senderId,
      recipientId = recipientId,
      amount = BigInteger.valueOf(amount),
      message = message,
      timestamp = timestamp,
      fingerprint = fingerprint
    )
    if (inserted == 0) logger.warn("Idempotent insert skipped duplicate transaction with fingerprint: $fingerprint")
  }

}
