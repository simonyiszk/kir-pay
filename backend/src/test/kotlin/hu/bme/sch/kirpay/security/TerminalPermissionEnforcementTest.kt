package hu.bme.sch.kirpay.security

import hu.bme.sch.kirpay.account.Account
import hu.bme.sch.kirpay.account.AccountRepository
import hu.bme.sch.kirpay.order.Item
import hu.bme.sch.kirpay.order.ItemRepository
import hu.bme.sch.kirpay.principal.Permission
import hu.bme.sch.kirpay.principal.Principal
import hu.bme.sch.kirpay.principal.PrincipalRepository
import hu.bme.sch.kirpay.principal.Role
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class TerminalPermissionEnforcementTest {

  @Autowired
  private lateinit var webApplicationContext: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var accountRepository: AccountRepository

  @Autowired
  private lateinit var itemRepository: ItemRepository

  @Autowired
  private lateinit var principalRepository: PrincipalRepository

  private val encoder = BCryptPasswordEncoder()

  @BeforeEach
  fun setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    accountRepository.deleteAll()
    itemRepository.deleteAll()
  }

  @AfterEach
  fun clearAuth() {
    SecurityContextHolder.clearContext()
  }

  private fun setAuth(principal: Principal, rawPassword: String) {
    val authorities = mutableListOf<SimpleGrantedAuthority>()
    authorities.add(SimpleGrantedAuthority("ROLE_${principal.role.name}"))
    if (principal.canSellItems) authorities.add(SimpleGrantedAuthority(Permission.SELL_ITEMS.name))
    if (principal.canRedeemVouchers) authorities.add(SimpleGrantedAuthority(Permission.REDEEM_VOUCHERS.name))
    if (principal.canUpload) authorities.add(SimpleGrantedAuthority(Permission.UPLOAD_FUNDS.name))
    if (principal.canTransfer) authorities.add(SimpleGrantedAuthority(Permission.TRANSFER_FUNDS.name))
    if (principal.canAssignCards) authorities.add(SimpleGrantedAuthority(Permission.ASSIGN_CARDS.name))
    val auth = UsernamePasswordAuthenticationToken(principal, rawPassword, authorities)
    SecurityContextHolder.getContext().authentication = auth
  }

  @Test
  fun `terminal without sell permission gets 403 on checkout`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "no-sell", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = false,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-CARD",
      balance = 1000,
      active = true))
    val item = itemRepository.save(Item(id = null, name = "Beer", alias = null, cost = 100, stock = 50, enabled = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-CARD/checkout")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":false}]}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without redeem vouchers gets 403`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "no-voucher", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = false, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-VCH",
      balance = 1000,
      active = true))
    val item = itemRepository.save(Item(id = null, name = "Item", alias = null, cost = 100, stock = 50, enabled = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-VCH/checkout")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":true}]}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without upload permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "no-upload", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = false, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-UP",
      balance = 1000,
      active = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-UP/upload")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":100}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without transfer permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "no-transfer", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = false, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-TX",
      balance = 1000,
      active = true))
    accountRepository.save(Account(id = null,
      name = "Recipient",
      email = null,
      phone = null,
      card = "SEC01-RX",
      balance = 100,
      active = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-TX/transfer")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"SEC01-RX","amount":100}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal without assign cards permission gets 403`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "no-assign", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = false,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    val account = accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = null,
      balance = 1000,
      active = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/accounts/${account.id}/card")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"NEW-CARD"}""")
    ).andExpect(status().isForbidden)
  }

  @Test
  fun `terminal with all permissions gets 200 on all endpoints`() {
    val rawPassword = "test-pw"
    val terminal = principalRepository.save(Principal(
      id = null, name = "all-perms", secret = encoder.encode(rawPassword)!!,
      role = Role.TERMINAL, active = true,
      canUpload = true, canTransfer = true, canSellItems = true,
      canRedeemVouchers = true, canAssignCards = true,
      createdAt = System.currentTimeMillis(), lastUsed = System.currentTimeMillis()
    ))
    accountRepository.save(Account(id = null,
      name = "Test",
      email = null,
      phone = null,
      card = "SEC01-ALL",
      balance = 10000,
      active = true))
    accountRepository.save(Account(id = null,
      name = "Recipient",
      email = null,
      phone = null,
      card = "SEC01-RECV",
      balance = 100,
      active = true))
    val item = itemRepository.save(Item(id = null, name = "Item", alias = null, cost = 100, stock = 50, enabled = true))
    setAuth(terminal, rawPassword)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-ALL/checkout")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"orderLines":[{"itemId":${item.id},"itemCount":1,"usedVoucher":false}]}""")
    ).andExpect(status().isOk)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-ALL/upload")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"amount":100}""")
    ).andExpect(status().isOk)

    mockMvc.perform(post("/v1/api/terminal/account-by-card/SEC01-RECV/transfer")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"recipientCard":"SEC01-ALL","amount":10}""")
    ).andExpect(status().isOk)

    val newAccount = accountRepository.save(Account(id = null,
      name = "New",
      email = null,
      phone = null,
      card = null,
      balance = 0,
      active = true))
    mockMvc.perform(post("/v1/api/terminal/accounts/${newAccount.id}/card")
      .contentType(MediaType.APPLICATION_JSON)
      .content("""{"card":"SEC01-NEWCARD"}""")
    ).andExpect(status().isOk)

    mockMvc.perform(get("/v1/api/terminal/account-by-card/SEC01-ALL"))
      .andExpect(status().isOk)
  }
}
