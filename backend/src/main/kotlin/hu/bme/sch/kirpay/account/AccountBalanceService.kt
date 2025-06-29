package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.common.RetryTransaction
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
class AccountBalanceService(
  private val accountRepository: AccountRepository,
  private val events: ApplicationEventPublisher,
  private val clock: Clock
) {

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun pay(card: String, amount: Long, logEvent: Boolean): Account {
    if (amount < 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountByCard(card)
      ?: throw BadRequestException("A számla nem létezik!")
    return pay(account, amount, logEvent)
  }


  fun pay(accountId: Int, amount: Long, logEvent: Boolean): Account {
    if (amount < 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountById(accountId)
      ?: throw BadRequestException("A számla nem létezik!")
    return pay(account, amount, logEvent)
  }


  fun pay(account: Account, amount: Long, logEvent: Boolean): Account {
    val newAccount = account.copy(balance = account.balance - amount)
    if (newAccount.balance < 0) throw BadRequestException("Nincs elég egyenleg!")

    accountRepository.save(newAccount)
    if (logEvent) {
      events.publishEvent(AccountPayEvent(newAccount, amount, getLoggedInPrincipal(), clock.millis()))
    }
    return newAccount
  }


  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun upload(card: String, amount: Long): Account {
    if (amount < 0) throw BadRequestException("Helytelen argumentum!")
    val account = accountRepository.findActiveAccountByCard(card)
      ?: throw BadRequestException("A számla nem létezik!")
    val newAccount = account.copy(balance = account.balance + amount)

    accountRepository.save(newAccount)
    events.publishEvent(AccountUploadEvent(newAccount, amount, getLoggedInPrincipal(), clock.millis()))
    return newAccount
  }


  // Returns the sender account
  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun transfer(senderCard: String, recipientCard: String, amount: Long): Account {
    if (amount < 0) throw BadRequestException("Helytelen argumentum!")

    val sender = accountRepository.findActiveAccountByCard(senderCard)
      ?: throw BadRequestException("A forrásszámla nem létezik!")
    val newSender = sender.copy(balance = sender.balance - amount)
    if (newSender.balance < 0) throw BadRequestException("Nincs elég egyenleg!")

    val recipient = accountRepository.findActiveAccountByCard(recipientCard)
      ?: throw BadRequestException("A célszámla nem létezik!")
    if (sender.id == recipient.id) throw BadRequestException("A küldő és fogadó nem lehet ugyanaz a személy!")
    val newRecipient = recipient.copy(balance = recipient.balance + amount)

    accountRepository.save(newSender)
    accountRepository.save(newRecipient)
    events.publishEvent(
      AccountBalanceTransferEvent(
        newSender,
        newRecipient,
        amount,
        getLoggedInPrincipal(),
        clock.millis()
      )
    )
    return newSender
  }

}
