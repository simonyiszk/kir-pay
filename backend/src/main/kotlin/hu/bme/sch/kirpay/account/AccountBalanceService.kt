package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.*
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import hu.bme.sch.kirpay.transaction.TransactionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.Clock
import java.util.*

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
class AccountBalanceService(
  private val accountRepository: AccountRepository,
  private val transactionService: TransactionService,
  private val eventService: EventService,
  private val clock: Clock,
  private val idempotencyService: IdempotencyService
) {
  data class MoneyMoveResult(val account: Account, val replayed: Boolean = false)

  private fun idempotentExecute(
    key: UUID,
    operationType: String,
    fingerprint: String,
    executeBlock: () -> Account
  ): MoneyMoveResult = idempotencyService.execute(key, operationType, fingerprint, Account::class, executeBlock)
    .let { MoneyMoveResult(it.value, it.replayed) }

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun pay(card: String, amount: Long, idempotencyKey: UUID, logEvent: Boolean): MoneyMoveResult {
    if (amount <= 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountByCard(card)
      ?: throw BadRequestException("A számla nem létezik!")
    return idempotentExecute(idempotencyKey,
      IdempotentOperationType.PAY,
      buildFingerprint(IdempotentOperationType.PAY, card, amount)) {
      pay(account, amount, logEvent)
    }
  }

  @RetryTransaction
  fun pay(accountId: Int, amount: Long, logEvent: Boolean): Account {
    if (amount <= 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountById(accountId)
      ?: throw BadRequestException("A számla nem létezik!")
    return pay(account, amount, logEvent)
  }

  fun pay(account: Account, amount: Long, logEvent: Boolean): Account {
    if (amount <= 0) throw BadRequestException("Helytelen argumentum!")
    val amt = BigInteger.valueOf(amount)
    val newBalance = account.balance.subtract(amt)
    if (newBalance < BigInteger.ZERO) throw BadRequestException("Nincs elég egyenleg!")
    val newAccount = account.copy(balance = newBalance)

    accountRepository.save(newAccount)
    if (logEvent) {
      transactionService.recordPay(newAccount.id!!, amount, clock.millis())
      eventService.logPay(newAccount, amount, getLoggedInPrincipal()?.toRef(), clock.millis())
    }
    return newAccount
  }

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun upload(card: String, amount: Long, idempotencyKey: UUID): MoneyMoveResult {
    if (amount <= 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountByCard(card)
      ?: throw BadRequestException("A számla nem létezik!")
    return idempotentExecute(idempotencyKey,
      IdempotentOperationType.UPLOAD,
      buildFingerprint(IdempotentOperationType.UPLOAD, card, amount)) {
      val newBalance = account.balance.add(BigInteger.valueOf(amount))
      val newAccount = account.copy(balance = newBalance)

      accountRepository.save(newAccount)
      eventService.logUpload(newAccount, amount, getLoggedInPrincipal()?.toRef(), clock.millis())
      transactionService.recordUpload(newAccount.id!!, amount, clock.millis())
      newAccount
    }
  }

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun transfer(senderCard: String, recipientCard: String, amount: Long, idempotencyKey: UUID): MoneyMoveResult {
    if (amount <= 0) throw BadRequestException("Helytelen argumentum!")

    val sender = accountRepository.findActiveAccountByCard(senderCard)
      ?: throw BadRequestException("A forrásszámla nem létezik!")
    val recipient = accountRepository.findActiveAccountByCard(recipientCard)
      ?: throw BadRequestException("A célszámla nem létezik!")
    if (sender.id == recipient.id) throw BadRequestException("A küldő és fogadó nem lehet ugyanaz a személy!")

    return idempotentExecute(idempotencyKey,
      IdempotentOperationType.TRANSFER,
      buildFingerprint(IdempotentOperationType.TRANSFER, senderCard, amount, recipientCard)) {
      val amt = BigInteger.valueOf(amount)
      val newSenderBalance = sender.balance.subtract(amt)
      if (newSenderBalance < BigInteger.ZERO) throw BadRequestException("Nincs elég egyenleg!")
      val newSender = sender.copy(balance = newSenderBalance)

      val newRecipient = recipient.copy(balance = recipient.balance.add(amt))

      accountRepository.save(newSender)
      accountRepository.save(newRecipient)
      transactionService.recordTransfer(newSender.id!!, newRecipient.id!!, amount, clock.millis())
      eventService.logBalanceTransfer(newSender, newRecipient, amount, getLoggedInPrincipal()?.toRef(), clock.millis())
      newSender
    }
  }

}
