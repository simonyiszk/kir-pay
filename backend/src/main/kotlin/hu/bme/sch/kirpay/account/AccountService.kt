package hu.bme.sch.kirpay.account

import hu.bme.sch.kirpay.common.*
import hu.bme.sch.kirpay.event.EventService
import hu.bme.sch.kirpay.principal.getLoggedInPrincipal
import hu.bme.sch.kirpay.principal.toRef
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.*

@Service
@Transactional
class AccountService(
  private val accountRepository: AccountRepository,
  private val eventService: EventService,
  private val clock: Clock,
  private val idempotencyService: IdempotencyService
) {
  fun find(id: Int): Account = accountRepository.findById(id).orElseThrow { NotFoundException("A számla nem létezik!") }

  fun findActive(id: Int): Account = accountRepository.findActiveAccountById(id)
    ?: throw NotFoundException("A számla nem létezik!")

  fun findAll(): List<Account> = accountRepository.findAllOrderByName().toList()

  fun findAllActive(): List<Account> = accountRepository.findAllActiveOrderByName().toList()

  @RetryTransaction
  fun create(dto: AccountCreateDto): Account = idempotencyService.execute(
    dto.idempotencyKey,
    IdempotentOperationType.ACCOUNT_CREATE,
    buildFingerprint(IdempotentOperationType.ACCOUNT_CREATE,
      dto.name, dto.email, dto.phone, dto.card, dto.balance, dto.active),
    Account::class
  ) {
    val account = try {
      accountRepository.saveAndFlush(dto.toAccount().copy(id = null))
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("A kártya, email vagy név már használatban van!")
    }
    eventService.logAccountCreated(account, getLoggedInPrincipal()?.toRef(), clock.millis())
    account
  }.value

  fun setEnabled(accountId: Int, active: Boolean): Account {
    val account = accountRepository.saveAndFlush(find(accountId).copy(active = active))
    eventService.logAccountUpdated(account, getLoggedInPrincipal()?.toRef(), clock.millis())
    return account
  }

  @RetryTransaction
  fun importAccounts(accounts: List<Account>, idempotencyKey: UUID, csv: String): BulkResult =
    idempotencyService.execute(
      idempotencyKey,
      IdempotentOperationType.ACCOUNT_IMPORT,
      buildFingerprint(IdempotentOperationType.ACCOUNT_IMPORT, csv),
      BulkResult::class
    ) {
      try {
        val saved = accountRepository.saveAll(accounts.map { it.copy(id = null) })
        accountRepository.flush()
        saved.forEach { eventService.logAccountCreated(it, getLoggedInPrincipal()?.toRef(), clock.millis()) }
        BulkResult(saved.size)
      } catch (e: DataIntegrityViolationException) {
        throw BadRequestException("A kártya, email vagy név már használatban van!")
      }
    }.value

  fun update(id: Int, dto: AccountUpdateDto): Account {
    if (!accountRepository.existsById(id)) throw BadRequestException("A számla nem létezik!")
    val existing = find(id)
    val account = try {
      accountRepository.saveAndFlush(dto.toAccount(id, existing.balance, existing.version))
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException("A kártya, email vagy név már használatban van!")
    }
    eventService.logAccountUpdated(account, getLoggedInPrincipal()?.toRef(), clock.millis())
    return account
  }

  fun deleteAccount(accountId: Int) {
    val account = find(accountId)
    if (accountRepository.safeDelete(accountId, account.version) == 0) {
      throw BadRequestException("A számla egyenlege nem nulla, vagy a számla időközben módosult, nem törölhető!")
    }
    eventService.logAccountDeleted(account, getLoggedInPrincipal()?.toRef(), clock.millis())
  }

  fun findActiveByCard(card: String): Account =
    accountRepository.findActiveAccountByCard(card) ?: throw NotFoundException("A kártyához nincs számla rendelve!")

  fun findActiveByEmail(email: String): Account =
    accountRepository.findActiveAccountByEmail(email)
      ?: throw NotFoundException("Nincs számla ilyen E-mail címmel rendelve!")

  @RetryTransaction
  @Transactional(isolation = Isolation.SERIALIZABLE)
  fun assignCard(accountId: Int, card: String): Account {
    val account = accountRepository.findById(accountId).orElseThrow { BadRequestException("A számla nem található!") }
    if (account.card == card) return account

    accountRepository.findByCard(card)?.let { holder ->
      if (holder.id == account.id) return@let

      val updatedHolder = holder.copy(card = null)
      // Flush so we don't trigger the unique index check
      accountRepository.saveAndFlush(updatedHolder)
      eventService.logAccountUpdated(updatedHolder, getLoggedInPrincipal()?.toRef(), clock.millis())
    }

    val newAccount = accountRepository.saveAndFlush(account.copy(card = card))
    assert(newAccount.card == card) { "Card assignment failed: expected '$card', got '${newAccount.card}'" }
    eventService.logAccountCardAssigned(newAccount, getLoggedInPrincipal()?.toRef(), clock.millis())
    return newAccount
  }

  fun countAll() = accountRepository.count()

  fun getAllActiveBalance() = accountRepository.getAllActiveBalance()

}
