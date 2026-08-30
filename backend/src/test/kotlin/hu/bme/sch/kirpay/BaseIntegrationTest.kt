package hu.bme.sch.kirpay

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.account.AccountRepository
import hu.bme.sch.kirpay.event.EventRepository
import hu.bme.sch.kirpay.order.Item
import hu.bme.sch.kirpay.order.ItemRepository
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRepository
import hu.bme.sch.kirpay.principal.Role
import hu.bme.sch.kirpay.principal.getPrincipalAuthorities
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class BaseIntegrationTest {

  @Autowired
  protected lateinit var accountRepository: AccountRepository

  @Autowired
  protected lateinit var itemRepository: ItemRepository

  @Autowired
  protected lateinit var principalRepository: PrincipalRepository

  @Autowired
  protected lateinit var eventRepository: EventRepository

  private val encoder = BCryptPasswordEncoder()

  @BeforeEach
  fun baseSetUp() {
    val principal = principalRepository.save(Principal(
      id = null, name = "test-terminal", secret = encoder.encode("test-pw")!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis(), version = 0
    ))
    val auth = UsernamePasswordAuthenticationToken(principal, "test-pw", getPrincipalAuthorities(principal))
    SecurityContextHolder.getContext().authentication = auth
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  protected fun createAccount(
    name: String = "Test User",
    card: String? = null,
    balance: Long = 5000,
    email: String? = null
  ): Account = accountRepository.save(
    Account(id = null,
      name = name,
      email = email,
      phone = null,
      card = card,
      balance = java.math.BigInteger.valueOf(balance),
      active = true)
  )

  protected fun createItem(
    name: String = "Test Item",
    cost: Long = 100,
    stock: Int = 50,
    enabled: Boolean = true
  ): Item = itemRepository.save(
    Item(id = null,
      name = name,
      alias = null,
      cost = java.math.BigInteger.valueOf(cost),
      stock = stock,
      enabled = enabled,
      showOnLeaderboard = false)
  )
}
