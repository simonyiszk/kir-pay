package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.BadRequestException
import hu.bme.sch.kirpay.common.NotFoundException
import hu.bme.sch.kirpay.common.RetryTransaction
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock


@Service
@Transactional
class AccountService(
  private val accountRepository: AccountRepository,
  private val events: ApplicationEventPublisher,
  private val clock: Clock
) {

  fun find(id: Int): Account = accountRepository.findById(id).orElseThrow { NotFoundException("A számla nem létezik!") }


  fun findAll(): List<Account> = accountRepository.findAllOrderByName().toList()


  fun create(dto: AccountCreateDto): Account {
    val account = accountRepository.save(dto.toAccount())
    events.publishEvent(AccountCreatedEvent(account, getLoggedInPrincipal(), clock.millis()))
    return account
  }


  fun setEnabled(accountId: Int, active: Boolean): Account {
    val account = accountRepository.save(find(accountId).copy(active = active))
    events.publishEvent(AccountUpdatedEvent(account, getLoggedInPrincipal(), clock.millis()))
    return account
  }


  fun importAccounts(accounts: List<Account>) = accountRepository.saveAll(accounts.map { it.copy(id = null) })
    .forEach { events.publishEvent(AccountCreatedEvent(it, getLoggedInPrincipal(), clock.millis())) }


  fun update(id: Int, dto: AccountUpdateDto): Account {
    if (!accountRepository.existsById(id)) throw BadRequestException("A számla nem létezik!")
    val account = accountRepository.save(dto.toAccount(id))
    events.publishEvent(AccountUpdatedEvent(account, getLoggedInPrincipal(), clock.millis()))
    return account
  }


  fun deleteAccount(accountId: Int) {
    val account = find(accountId)
    accountRepository.deleteById(accountId)
    events.publishEvent(AccountDeletedEvent(account, getLoggedInPrincipal(), clock.millis()))
  }

  fun findActiveByCard(card: String): Account =
    accountRepository.findActiveAccountByCard(card) ?: throw NotFoundException("A kártyához nincs számla rendelve!")


  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun assignCard(accountId: Int, card: String): Account {
    val account = accountRepository.findById(accountId).orElseThrow { BadRequestException("A számla nem található!") }
    if (account.card == card) return account

    accountRepository.findByCard(card)?.let { holder ->
      if (holder.id == account.id) return@let

      val updatedHolder = holder.copy(card = null)
      accountRepository.save(updatedHolder)
      events.publishEvent(AccountUpdatedEvent(updatedHolder, getLoggedInPrincipal(), clock.millis()))
    }

    val newAccount = accountRepository.save(account.copy(card = card))
    events.publishEvent(AccountCardAssignedEvent(newAccount, getLoggedInPrincipal(), clock.millis()))
    return newAccount
  }


  fun countAll() = accountRepository.count()

  fun getAllActiveBalance() = accountRepository.getAllActiveBalance()

}
